package com.gov.ac.feature.organization.service;

import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.feature.departments.entity.DepartmentEntity;
import com.gov.ac.feature.departments.repository.DepartmentRepository;
import com.gov.ac.feature.organization.dto.RoutingChainDto;
import com.gov.ac.feature.organization.dto.RoutingStopDto;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pure-business routing service for the Saudi MoD Q/L/K/S hierarchy.
 *
 * <p>Rules (computed from {@code department.level_code}, not hardcoded department ids):
 *
 * <ul>
 *   <li><b>Q can dispatch directly to any level.</b> Chain = [target].
 *   <li><b>Same K, S->S</b>: peers within one battalion route directly. Chain = [target].
 *   <li><b>Same L, different K</b>: route must climb to the receiving K. Chain = [originator.K,
 *       target.K, target].
 *   <li><b>Different L</b>: route must climb to Q and back down. Chain = [originator.K,
 *       originator.L, Q, target.L, target.K, target].
 * </ul>
 *
 * <p>The output is consumed by Camunda multi-instance subprocesses (one user task per stop) and
 * by the FE routing-preview endpoint. The service is read-only; callers persist the result on
 * {@code workflow_instance.routing_chain_json}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrgRoutingService {

  /** Q/L/K/S level codes (canonical, upper-case). */
  public static final String LEVEL_Q = "Q";

  public static final String LEVEL_L = "L";
  public static final String LEVEL_K = "K";
  public static final String LEVEL_S = "S";

  /** Default role expected per level (overridable later by org admin). */
  private final DepartmentRepository departmentRepository;
  private final OrgLevelRoleResolver orgLevelRoleResolver;

  @Transactional(readOnly = true)
  public RoutingChainDto computeChain(Long originatorDepartmentId, Long targetDepartmentId) {
    Objects.requireNonNull(originatorDepartmentId, "originatorDepartmentId is required");
    Objects.requireNonNull(targetDepartmentId, "targetDepartmentId is required");

    DepartmentEntity originator = loadDepartment(originatorDepartmentId);
    DepartmentEntity target = loadDepartment(targetDepartmentId);

    Map<String, DepartmentEntity> originatorAncestors = ancestorsByLevel(originator);
    Map<String, DepartmentEntity> targetAncestors = ancestorsByLevel(target);

    // Originator is Q (or directly under Q) -> direct dispatch.
    if (isLevel(originator, LEVEL_Q)) {
      return chain(originator, target, List.of(toStop(target, target.getLevelCode())),
          "routing.fromHeadquarters");
    }

    // Same battalion (K) and both endpoints at S level -> direct peer route.
    DepartmentEntity originatorK = originatorAncestors.get(LEVEL_K);
    DepartmentEntity targetK = targetAncestors.get(LEVEL_K);
    if (originatorK != null && targetK != null && Objects.equals(originatorK.getId(), targetK.getId())
        && isLevel(originator, LEVEL_S) && isLevel(target, LEVEL_S)) {
      return chain(originator, target, List.of(toStop(target, LEVEL_S)), "routing.sameUnit");
    }

    // Same brigade (L), different battalions -> bounce through receiving K.
    DepartmentEntity originatorL = originatorAncestors.get(LEVEL_L);
    DepartmentEntity targetL = targetAncestors.get(LEVEL_L);
    if (originatorL != null && targetL != null && Objects.equals(originatorL.getId(), targetL.getId())
        && originatorK != null && targetK != null
        && !Objects.equals(originatorK.getId(), targetK.getId())) {
      List<RoutingStopDto> stops = new ArrayList<>();
      stops.add(toStop(originatorK, LEVEL_K));
      stops.add(toStop(targetK, LEVEL_K));
      stops.add(toStop(target, target.getLevelCode()));
      return chain(originator, target, stops, "routing.viaParent");
    }

    // Different brigade (L) -> climb to Q, then descend.
    List<RoutingStopDto> stops = new ArrayList<>();
    if (originatorK != null) {
      stops.add(toStop(originatorK, LEVEL_K));
    }
    if (originatorL != null) {
      stops.add(toStop(originatorL, LEVEL_L));
    }
    DepartmentEntity hq = findQAncestor(originator);
    if (hq != null) {
      stops.add(toStop(hq, LEVEL_Q));
    }
    if (targetL != null) {
      stops.add(toStop(targetL, LEVEL_L));
    }
    if (targetK != null) {
      stops.add(toStop(targetK, LEVEL_K));
    }
    stops.add(toStop(target, target.getLevelCode()));
    return chain(originator, target, dedupe(stops), "routing.viaHeadquarters");
  }

  // ---------------------------------------------------------------------------
  // helpers
  // ---------------------------------------------------------------------------

  private DepartmentEntity loadDepartment(Long id) {
    return departmentRepository
        .findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new NotFoundException("Department not found: " + id));
  }

  /**
   * Walks the parent chain and indexes ancestors by level code. Includes the node itself if it
   * has a level code (so for an S leaf, the map contains S = node, K = parent.K, etc.).
   */
  private Map<String, DepartmentEntity> ancestorsByLevel(DepartmentEntity node) {
    Map<String, DepartmentEntity> byLevel = new LinkedHashMap<>();
    DepartmentEntity cursor = node;
    int safety = 32;
    while (cursor != null && safety-- > 0) {
      String level = cursor.getLevelCode();
      if (level != null && !byLevel.containsKey(level)) {
        byLevel.put(level, cursor);
      }
      cursor = cursor.getParent();
    }
    return byLevel;
  }

  private DepartmentEntity findQAncestor(DepartmentEntity node) {
    Map<String, DepartmentEntity> map = ancestorsByLevel(node);
    return map.get(LEVEL_Q);
  }

  private static boolean isLevel(DepartmentEntity node, String level) {
    return node != null && level.equalsIgnoreCase(node.getLevelCode());
  }

  private RoutingStopDto toStop(DepartmentEntity dept, String levelOverride) {
    String level = levelOverride != null ? levelOverride : dept.getLevelCode();
    String role = orgLevelRoleResolver.resolveRoleCode(level);
    return new RoutingStopDto(
        dept.getId(),
        dept.getCode(),
        dept.getNameAr(),
        dept.getNameEn(),
        level,
        role,
        "routing.stop." + (level == null ? "unknown" : level.toLowerCase()));
  }

  private RoutingChainDto chain(
      DepartmentEntity originator,
      DepartmentEntity target,
      List<RoutingStopDto> stops,
      String reasonKey) {
    log.debug(
        "OrgRouting: {} -> {} via {} stops ({})",
        originator.getCode(),
        target.getCode(),
        stops.size(),
        reasonKey);
    return new RoutingChainDto(
        toStop(originator, originator.getLevelCode()),
        toStop(target, target.getLevelCode()),
        stops,
        reasonKey);
  }

  private static List<RoutingStopDto> dedupe(List<RoutingStopDto> stops) {
    List<RoutingStopDto> out = new ArrayList<>(stops.size());
    Long lastId = null;
    for (RoutingStopDto stop : stops) {
      if (stop == null) {
        continue;
      }
      if (!Objects.equals(stop.departmentId(), lastId)) {
        out.add(stop);
        lastId = stop.departmentId();
      }
    }
    return out;
  }
}
