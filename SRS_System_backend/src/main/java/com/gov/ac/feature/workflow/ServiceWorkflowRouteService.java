package com.gov.ac.feature.workflow;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.domain.lookup.CorrespondenceType;
import com.gov.ac.domain.workflow.ServiceWorkflowRoute;
import com.gov.ac.feature.workflow.dto.ServiceWorkflowRouteDto;
import com.gov.ac.feature.workflow.dto.UpsertServiceWorkflowRouteRequest;
import com.gov.ac.persistence.CorrespondenceTypeRepository;
import com.gov.ac.persistence.ServiceWorkflowRouteRepository;
import com.gov.ac.security.SecurityUtils;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ServiceWorkflowRouteService {

  private final ServiceWorkflowRouteRepository routeRepository;
  private final CorrespondenceTypeRepository correspondenceTypeRepository;

  @Transactional(readOnly = true)
  public List<ServiceWorkflowRouteDto> listActiveForTypeCode(String correspondenceTypeCode) {
    if (!StringUtils.hasText(correspondenceTypeCode)) {
      throw new BadRequestException("correspondenceTypeCode is required");
    }
    CorrespondenceType type =
        correspondenceTypeRepository
            .findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(correspondenceTypeCode.trim())
            .orElseThrow(() -> new BadRequestException("Unknown correspondence type"));
    return routeRepository.findActiveRoutesForType(type.getId()).stream()
        .map(ServiceWorkflowRouteService::toDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ServiceWorkflowRouteDto> listAllForAdmin() {
    return routeRepository.findByDeletedAtIsNullOrderByCorrespondenceTypeIdAscSortOrderAsc().stream()
        .map(ServiceWorkflowRouteService::toDto)
        .toList();
  }

  @Transactional
  public ServiceWorkflowRouteDto create(UpsertServiceWorkflowRouteRequest req) {
    UUID actor = SecurityUtils.requireCurrentUserId();
    CorrespondenceType type = loadType(req.correspondenceTypeId());
    if (req.defaultRoute()) {
      clearOtherDefaults(type.getId(), null);
    }
    ServiceWorkflowRoute r = new ServiceWorkflowRoute();
    r.setCorrespondenceType(type);
    applyUpsert(r, req, actor);
    r = routeRepository.save(r);
    return toDto(r);
  }

  @Transactional
  public ServiceWorkflowRouteDto update(long id, UpsertServiceWorkflowRouteRequest req) {
    UUID actor = SecurityUtils.requireCurrentUserId();
    ServiceWorkflowRoute r =
        routeRepository
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Workflow route not found"));
    CorrespondenceType type = loadType(req.correspondenceTypeId());
    if (req.defaultRoute()) {
      clearOtherDefaults(type.getId(), id);
    }
    r.setCorrespondenceType(type);
    applyUpsert(r, req, actor);
    r = routeRepository.save(r);
    return toDto(r);
  }

  @Transactional
  public void delete(long id) {
    UUID actor = SecurityUtils.requireCurrentUserId();
    ServiceWorkflowRoute r =
        routeRepository
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Workflow route not found"));
    r.setDeletedAt(java.time.Instant.now());
    r.setDeletedBy(actor);
    r.setUpdatedBy(actor);
    routeRepository.save(r);
  }

  private void applyUpsert(ServiceWorkflowRoute r, UpsertServiceWorkflowRouteRequest req, UUID actor) {
    r.setProcessDefinitionKey(req.processDefinitionKey().trim());
    r.setNameAr(req.nameAr().trim());
    r.setNameEn(req.nameEn().trim());
    r.setDefaultRoute(req.defaultRoute());
    r.setSortOrder(req.sortOrder());
    r.setActive(req.active());
    if (r.getCreatedBy() == null) {
      r.setCreatedBy(actor);
    }
    r.setUpdatedBy(actor);
  }

  private void clearOtherDefaults(long correspondenceTypeId, Long exceptRouteId) {
    List<ServiceWorkflowRoute> rows =
        routeRepository.findByCorrespondenceTypeIdAndDeletedAtIsNullOrderBySortOrderAsc(
            correspondenceTypeId);
    for (ServiceWorkflowRoute row : rows) {
      if (exceptRouteId != null && row.getId().equals(exceptRouteId)) {
        continue;
      }
      if (row.isDefaultRoute()) {
        row.setDefaultRoute(false);
        row.setUpdatedBy(SecurityUtils.requireCurrentUserId());
        routeRepository.save(row);
      }
    }
  }

  private CorrespondenceType loadType(Long id) {
    return correspondenceTypeRepository
        .findById(id)
        .filter(t -> t.getDeletedAt() == null && Boolean.TRUE.equals(t.getActive()))
        .orElseThrow(() -> new BadRequestException("Unknown correspondence type"));
  }

  private static ServiceWorkflowRouteDto toDto(ServiceWorkflowRoute r) {
    CorrespondenceType t = r.getCorrespondenceType();
    return new ServiceWorkflowRouteDto(
        r.getId(),
        t != null ? t.getId() : 0L,
        t != null ? t.getCode() : "",
        r.getProcessDefinitionKey(),
        r.getNameAr(),
        r.getNameEn(),
        r.isDefaultRoute(),
        r.getSortOrder() != null ? r.getSortOrder() : 0,
        Boolean.TRUE.equals(r.getActive()));
  }
}
