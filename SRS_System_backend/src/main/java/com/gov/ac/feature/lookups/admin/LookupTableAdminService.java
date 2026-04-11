package com.gov.ac.feature.lookups.admin;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.domain.base.SoftDeletableEntity;
import com.gov.ac.domain.lookup.Confidentiality;
import com.gov.ac.domain.lookup.CorrespondenceStatus;
import com.gov.ac.domain.lookup.CorrespondenceType;
import com.gov.ac.domain.lookup.OrgVisualNodeStatus;
import com.gov.ac.domain.lookup.Priority;
import com.gov.ac.domain.lookup.WorkflowActionType;
import com.gov.ac.domain.lookup.WorkflowHistoryEventType;
import com.gov.ac.domain.lookup.LookupCatalog;
import com.gov.ac.domain.org.Classification;
import com.gov.ac.feature.lookups.admin.dto.LookupCatalogDto;
import com.gov.ac.feature.lookups.admin.dto.LookupRowAdminDto;
import com.gov.ac.feature.lookups.admin.dto.LookupUpsertRequest;
import com.gov.ac.persistence.ClassificationRepository;
import com.gov.ac.persistence.ConfidentialityRepository;
import com.gov.ac.persistence.CorrespondenceStatusRepository;
import com.gov.ac.persistence.CorrespondenceTypeRepository;
import com.gov.ac.persistence.LookupCatalogRepository;
import com.gov.ac.persistence.OrgVisualNodeStatusRepository;
import com.gov.ac.persistence.PriorityRepository;
import com.gov.ac.persistence.WorkflowActionTypeRepository;
import com.gov.ac.persistence.WorkflowHistoryEventTypeRepository;
import com.gov.ac.security.SecurityUtils;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LookupTableAdminService {

  private static final Set<String> MANAGED =
      Set.of(
          "correspondence_type",
          "correspondence_status",
          "priority",
          "confidentiality",
          "classification",
          "workflow_action_type",
          "workflow_history_event_type",
          "org_visual_node_status");

  private final LookupCatalogRepository lookupCatalogRepository;
  private final CorrespondenceTypeRepository correspondenceTypeRepository;
  private final CorrespondenceStatusRepository correspondenceStatusRepository;
  private final PriorityRepository priorityRepository;
  private final ConfidentialityRepository confidentialityRepository;
  private final ClassificationRepository classificationRepository;
  private final WorkflowActionTypeRepository workflowActionTypeRepository;
  private final WorkflowHistoryEventTypeRepository workflowHistoryEventTypeRepository;
  private final OrgVisualNodeStatusRepository orgVisualNodeStatusRepository;

  @Transactional(readOnly = true)
  public List<LookupCatalogDto> listCatalog() {
    return lookupCatalogRepository.findAllByOrderBySortOrderAsc().stream()
        .map(LookupTableAdminService::toCatalogDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<LookupRowAdminDto> listRows(String lookupCode) {
    requireManaged(lookupCode);
    return switch (lookupCode) {
      case "correspondence_type" ->
          correspondenceTypeRepository.findByDeletedAtIsNullOrderBySortOrderAsc().stream()
              .map(t -> mapType(t, lookupCode))
              .toList();
      case "correspondence_status" ->
          correspondenceStatusRepository.findByDeletedAtIsNullOrderBySortOrderAsc().stream()
              .map(s -> mapStatus(s, lookupCode))
              .toList();
      case "priority" ->
          priorityRepository.findByDeletedAtIsNullOrderBySortOrderAsc().stream()
              .map(p -> mapPriority(p, lookupCode))
              .toList();
      case "confidentiality" ->
          confidentialityRepository.findByDeletedAtIsNullOrderBySortOrderAsc().stream()
              .map(c -> mapConf(c, lookupCode))
              .toList();
      case "classification" ->
          classificationRepository.findByDeletedAtIsNullOrderBySortOrderAsc().stream()
              .map(c -> mapClassification(c, lookupCode))
              .toList();
      case "workflow_action_type" ->
          workflowActionTypeRepository.findByDeletedAtIsNullOrderBySortOrderAsc().stream()
              .map(a -> mapWfAction(a, lookupCode))
              .toList();
      case "workflow_history_event_type" ->
          workflowHistoryEventTypeRepository.findByDeletedAtIsNullOrderBySortOrderAsc().stream()
              .map(e -> mapWfEvent(e, lookupCode))
              .toList();
      case "org_visual_node_status" ->
          orgVisualNodeStatusRepository.findByDeletedAtIsNullOrderBySortOrderAsc().stream()
              .map(s -> mapOrgVisualNode(s, lookupCode))
              .toList();
      default -> throw new NotFoundException("Unknown lookup");
    };
  }

  @Transactional
  public LookupRowAdminDto create(String lookupCode, LookupUpsertRequest req) {
    requireManaged(lookupCode);
    UUID uid = SecurityUtils.requireCurrentUserId();
    return switch (lookupCode) {
      case "correspondence_type" -> mapType(createType(req, uid), lookupCode);
      case "correspondence_status" -> mapStatus(createStatus(req, uid), lookupCode);
      case "priority" -> mapPriority(createPriority(req, uid), lookupCode);
      case "confidentiality" -> mapConf(createConf(req, uid), lookupCode);
      case "classification" -> mapClassification(createClassification(req, uid), lookupCode);
      case "workflow_action_type" -> mapWfAction(createWfAction(req, uid), lookupCode);
      case "workflow_history_event_type" -> mapWfEvent(createWfEvent(req, uid), lookupCode);
      case "org_visual_node_status" -> mapOrgVisualNode(createOrgVisualNode(req, uid), lookupCode);
      default -> throw new NotFoundException("Unknown lookup");
    };
  }

  @Transactional
  public LookupRowAdminDto update(String lookupCode, Long id, LookupUpsertRequest req) {
    requireManaged(lookupCode);
    UUID uid = SecurityUtils.requireCurrentUserId();
    return switch (lookupCode) {
      case "correspondence_type" -> mapType(updateType(id, req, uid), lookupCode);
      case "correspondence_status" -> mapStatus(updateStatus(id, req, uid), lookupCode);
      case "priority" -> mapPriority(updatePriority(id, req, uid), lookupCode);
      case "confidentiality" -> mapConf(updateConf(id, req, uid), lookupCode);
      case "classification" -> mapClassification(updateClassification(id, req, uid), lookupCode);
      case "workflow_action_type" -> mapWfAction(updateWfAction(id, req, uid), lookupCode);
      case "workflow_history_event_type" -> mapWfEvent(updateWfEvent(id, req, uid), lookupCode);
      case "org_visual_node_status" -> mapOrgVisualNode(updateOrgVisualNode(id, req, uid), lookupCode);
      default -> throw new NotFoundException("Unknown lookup");
    };
  }

  @Transactional
  public void delete(String lookupCode, Long id) {
    requireManaged(lookupCode);
    UUID uid = SecurityUtils.requireCurrentUserId();
    Instant now = Instant.now();
    switch (lookupCode) {
      case "correspondence_type" -> {
        CorrespondenceType t =
            correspondenceTypeRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("Row not found"));
        t.setDeletedAt(now);
        t.setDeletedBy(uid);
        t.setUpdatedBy(uid);
      }
      case "correspondence_status" -> {
        CorrespondenceStatus s =
            correspondenceStatusRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("Row not found"));
        s.setDeletedAt(now);
        s.setDeletedBy(uid);
        s.setUpdatedBy(uid);
      }
      case "priority" ->
          markDeleted(
              priorityRepository
                  .findByIdAndDeletedAtIsNull(id)
                  .orElseThrow(() -> new NotFoundException("Row not found")),
              uid,
              now);
      case "confidentiality" ->
          markDeleted(
              confidentialityRepository
                  .findByIdAndDeletedAtIsNull(id)
                  .orElseThrow(() -> new NotFoundException("Row not found")),
              uid,
              now);
      case "classification" ->
          markDeleted(
              classificationRepository
                  .findByIdAndDeletedAtIsNull(id)
                  .orElseThrow(() -> new NotFoundException("Row not found")),
              uid,
              now);
      case "workflow_action_type" ->
          markDeleted(
              workflowActionTypeRepository
                  .findByIdAndDeletedAtIsNull(id)
                  .orElseThrow(() -> new NotFoundException("Row not found")),
              uid,
              now);
      case "workflow_history_event_type" ->
          markDeleted(
              workflowHistoryEventTypeRepository
                  .findByIdAndDeletedAtIsNull(id)
                  .orElseThrow(() -> new NotFoundException("Row not found")),
              uid,
              now);
      case "org_visual_node_status" ->
          markDeleted(
              orgVisualNodeStatusRepository
                  .findByIdAndDeletedAtIsNull(id)
                  .orElseThrow(() -> new NotFoundException("Row not found")),
              uid,
              now);
      default -> throw new NotFoundException("Unknown lookup");
    }
  }

  private static void markDeleted(SoftDeletableEntity e, UUID uid, Instant now) {
    e.setDeletedAt(now);
    e.setDeletedBy(uid);
    e.setUpdatedBy(uid);
  }

  private void requireManaged(String lookupCode) {
    if (!MANAGED.contains(lookupCode)) {
      throw new NotFoundException("Unknown lookup code");
    }
    if (!lookupCatalogRepository.existsById(lookupCode)) {
      throw new NotFoundException("Lookup is not registered in lookup_catalog");
    }
  }

  private CorrespondenceType createType(LookupUpsertRequest req, UUID uid) {
    String code = req.code().trim();
    if (correspondenceTypeRepository.existsByCodeIgnoreCaseAndDeletedAtIsNull(code)) {
      throw new BadRequestException("Duplicate code");
    }
    CorrespondenceType t = new CorrespondenceType();
    applyCommon(t, req, uid);
    t.setCode(code);
    t.setCreatedBy(uid);
    t.setUpdatedBy(uid);
    return correspondenceTypeRepository.save(t);
  }

  private CorrespondenceType updateType(Long id, LookupUpsertRequest req, UUID uid) {
    CorrespondenceType t =
        correspondenceTypeRepository
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Row not found"));
    String code = req.code().trim();
    if (correspondenceTypeRepository.existsByCodeIgnoreCaseAndIdNotAndDeletedAtIsNull(code, id)) {
      throw new BadRequestException("Duplicate code");
    }
    t.setCode(code);
    applyCommon(t, req, uid);
    t.setUpdatedBy(uid);
    return correspondenceTypeRepository.save(t);
  }

  private CorrespondenceStatus createStatus(LookupUpsertRequest req, UUID uid) {
    String code = req.code().trim();
    assertStatusCodeUnique(null, req.parentId(), code);
    CorrespondenceStatus s = new CorrespondenceStatus();
    s.setCode(code);
    s.setNameAr(req.nameAr().trim());
    s.setNameEn(req.nameEn().trim());
    s.setDescription(req.description());
    s.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
    s.setActive(req.active() != null ? req.active() : true);
    s.setTerminal(Boolean.TRUE.equals(req.terminal()));
    if (req.parentId() != null) {
      CorrespondenceType type =
          correspondenceTypeRepository
              .findById(req.parentId())
              .filter(x -> x.getDeletedAt() == null)
              .orElseThrow(() -> new BadRequestException("Unknown correspondence type"));
      s.setCorrespondenceType(type);
    } else {
      s.setCorrespondenceType(null);
    }
    s.setUiVariant(normalizeCorrespondenceStatusUiVariant(req.uiVariant()));
    s.setCreatedBy(uid);
    s.setUpdatedBy(uid);
    return correspondenceStatusRepository.save(s);
  }

  private CorrespondenceStatus updateStatus(Long id, LookupUpsertRequest req, UUID uid) {
    CorrespondenceStatus s =
        correspondenceStatusRepository
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Row not found"));
    String code = req.code().trim();
    assertStatusCodeUnique(id, req.parentId(), code);
    s.setCode(code);
    s.setNameAr(req.nameAr().trim());
    s.setNameEn(req.nameEn().trim());
    s.setDescription(req.description());
    s.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
    s.setActive(req.active() != null ? req.active() : true);
    s.setTerminal(Boolean.TRUE.equals(req.terminal()));
    if (req.parentId() != null) {
      CorrespondenceType type =
          correspondenceTypeRepository
              .findById(req.parentId())
              .filter(x -> x.getDeletedAt() == null)
              .orElseThrow(() -> new BadRequestException("Unknown correspondence type"));
      s.setCorrespondenceType(type);
    } else {
      s.setCorrespondenceType(null);
    }
    s.setUiVariant(normalizeCorrespondenceStatusUiVariant(req.uiVariant()));
    s.setUpdatedBy(uid);
    return correspondenceStatusRepository.save(s);
  }

  private static String normalizeCorrespondenceStatusUiVariant(String raw) {
    if (raw == null || raw.isBlank()) {
      return "neutral";
    }
    String v = raw.trim().toLowerCase();
    if (java.util.Set.of("success", "danger", "warning", "info", "secondary", "neutral").contains(v)) {
      return v;
    }
    return "neutral";
  }

  private void assertStatusCodeUnique(Long idOrNull, Long typeId, String code) {
    if (typeId == null) {
      boolean dup =
          idOrNull == null
              ? correspondenceStatusRepository
                  .existsByCorrespondenceTypeIsNullAndCodeIgnoreCaseAndDeletedAtIsNull(code)
              : correspondenceStatusRepository
                  .existsByCorrespondenceTypeIsNullAndCodeIgnoreCaseAndIdNotAndDeletedAtIsNull(
                      code, idOrNull);
      if (dup) {
        throw new BadRequestException("Duplicate code for global status");
      }
    } else {
      boolean dup =
          idOrNull == null
              ? correspondenceStatusRepository
                  .existsByCorrespondenceType_IdAndCodeIgnoreCaseAndDeletedAtIsNull(typeId, code)
              : correspondenceStatusRepository
                  .existsByCorrespondenceType_IdAndCodeIgnoreCaseAndIdNotAndDeletedAtIsNull(
                      typeId, code, idOrNull);
      if (dup) {
        throw new BadRequestException("Duplicate code for this correspondence type");
      }
    }
  }

  private Priority createPriority(LookupUpsertRequest req, UUID uid) {
    String code = req.code().trim();
    if (priorityRepository.existsByCodeIgnoreCaseAndDeletedAtIsNull(code)) {
      throw new BadRequestException("Duplicate code");
    }
    Priority p = new Priority();
    p.setCode(code);
    p.setNameAr(req.nameAr().trim());
    p.setNameEn(req.nameEn().trim());
    p.setDescription(req.description());
    p.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
    p.setActive(req.active() != null ? req.active() : true);
    p.setSlaDays(req.slaDays());
    p.setCreatedBy(uid);
    p.setUpdatedBy(uid);
    return priorityRepository.save(p);
  }

  private Priority updatePriority(Long id, LookupUpsertRequest req, UUID uid) {
    Priority p =
        priorityRepository.findByIdAndDeletedAtIsNull(id).orElseThrow(() -> new NotFoundException("Row not found"));
    String code = req.code().trim();
    if (priorityRepository.existsByCodeIgnoreCaseAndIdNotAndDeletedAtIsNull(code, id)) {
      throw new BadRequestException("Duplicate code");
    }
    p.setCode(code);
    p.setNameAr(req.nameAr().trim());
    p.setNameEn(req.nameEn().trim());
    p.setDescription(req.description());
    p.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
    p.setActive(req.active() != null ? req.active() : true);
    p.setSlaDays(req.slaDays());
    p.setUpdatedBy(uid);
    return priorityRepository.save(p);
  }

  private Confidentiality createConf(LookupUpsertRequest req, UUID uid) {
    String code = req.code().trim();
    if (confidentialityRepository.existsByCodeIgnoreCaseAndDeletedAtIsNull(code)) {
      throw new BadRequestException("Duplicate code");
    }
    Confidentiality c = new Confidentiality();
    c.setCode(code);
    c.setNameAr(req.nameAr().trim());
    c.setNameEn(req.nameEn().trim());
    c.setDescription(req.description());
    c.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
    c.setActive(req.active() != null ? req.active() : true);
    c.setRestrictsExport(Boolean.TRUE.equals(req.restrictsExport()));
    c.setRequiresClearance(Boolean.TRUE.equals(req.requiresClearance()));
    c.setCreatedBy(uid);
    c.setUpdatedBy(uid);
    return confidentialityRepository.save(c);
  }

  private Confidentiality updateConf(Long id, LookupUpsertRequest req, UUID uid) {
    Confidentiality c =
        confidentialityRepository
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Row not found"));
    String code = req.code().trim();
    if (confidentialityRepository.existsByCodeIgnoreCaseAndIdNotAndDeletedAtIsNull(code, id)) {
      throw new BadRequestException("Duplicate code");
    }
    c.setCode(code);
    c.setNameAr(req.nameAr().trim());
    c.setNameEn(req.nameEn().trim());
    c.setDescription(req.description());
    c.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
    c.setActive(req.active() != null ? req.active() : true);
    c.setRestrictsExport(Boolean.TRUE.equals(req.restrictsExport()));
    c.setRequiresClearance(Boolean.TRUE.equals(req.requiresClearance()));
    c.setUpdatedBy(uid);
    return confidentialityRepository.save(c);
  }

  private Classification createClassification(LookupUpsertRequest req, UUID uid) {
    String code = req.code().trim();
    assertClassificationCodeUnique(null, req.parentId(), code);
    Classification c = new Classification();
    c.setCode(code);
    c.setNameAr(req.nameAr().trim());
    c.setNameEn(req.nameEn().trim());
    c.setDescription(req.description());
    c.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
    c.setActive(req.active() != null ? req.active() : true);
    if (req.parentId() != null) {
      Classification parent =
          classificationRepository
              .findByIdAndDeletedAtIsNull(req.parentId())
              .orElseThrow(() -> new BadRequestException("Unknown parent classification"));
      c.setParent(parent);
    }
    c.setCreatedBy(uid);
    c.setUpdatedBy(uid);
    return classificationRepository.save(c);
  }

  private Classification updateClassification(Long id, LookupUpsertRequest req, UUID uid) {
    Classification c =
        classificationRepository
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Row not found"));
    String code = req.code().trim();
    assertClassificationCodeUnique(id, req.parentId(), code);
    if (req.parentId() != null && req.parentId().equals(id)) {
      throw new BadRequestException("Classification cannot be its own parent");
    }
    c.setCode(code);
    c.setNameAr(req.nameAr().trim());
    c.setNameEn(req.nameEn().trim());
    c.setDescription(req.description());
    c.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
    c.setActive(req.active() != null ? req.active() : true);
    if (req.parentId() != null) {
      Classification parent =
          classificationRepository
              .findByIdAndDeletedAtIsNull(req.parentId())
              .orElseThrow(() -> new BadRequestException("Unknown parent classification"));
      c.setParent(parent);
    } else {
      c.setParent(null);
    }
    c.setUpdatedBy(uid);
    return classificationRepository.save(c);
  }

  private void assertClassificationCodeUnique(Long idOrNull, Long parentId, String code) {
    if (parentId == null) {
      boolean dup =
          idOrNull == null
              ? classificationRepository.existsByParentIsNullAndCodeIgnoreCaseAndDeletedAtIsNull(code)
              : classificationRepository.existsByParentIsNullAndCodeIgnoreCaseAndIdNotAndDeletedAtIsNull(
                  code, idOrNull);
      if (dup) {
        throw new BadRequestException("Duplicate code at root level");
      }
    } else {
      boolean dup =
          idOrNull == null
              ? classificationRepository.existsByParent_IdAndCodeIgnoreCaseAndDeletedAtIsNull(
                  parentId, code)
              : classificationRepository.existsByParent_IdAndCodeIgnoreCaseAndIdNotAndDeletedAtIsNull(
                  parentId, code, idOrNull);
      if (dup) {
        throw new BadRequestException("Duplicate code under this parent");
      }
    }
  }

  private WorkflowActionType createWfAction(LookupUpsertRequest req, UUID uid) {
    String code = req.code().trim();
    if (workflowActionTypeRepository.existsByCodeIgnoreCaseAndDeletedAtIsNull(code)) {
      throw new BadRequestException("Duplicate code");
    }
    WorkflowActionType a = new WorkflowActionType();
    a.setCode(code);
    a.setNameAr(req.nameAr().trim());
    a.setNameEn(req.nameEn().trim());
    a.setDescription(req.description());
    a.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
    a.setActive(req.active() != null ? req.active() : true);
    a.setCreatedBy(uid);
    a.setUpdatedBy(uid);
    return workflowActionTypeRepository.save(a);
  }

  private WorkflowActionType updateWfAction(Long id, LookupUpsertRequest req, UUID uid) {
    WorkflowActionType a =
        workflowActionTypeRepository
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Row not found"));
    String code = req.code().trim();
    if (workflowActionTypeRepository.existsByCodeIgnoreCaseAndIdNotAndDeletedAtIsNull(code, id)) {
      throw new BadRequestException("Duplicate code");
    }
    a.setCode(code);
    a.setNameAr(req.nameAr().trim());
    a.setNameEn(req.nameEn().trim());
    a.setDescription(req.description());
    a.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
    a.setActive(req.active() != null ? req.active() : true);
    a.setUpdatedBy(uid);
    return workflowActionTypeRepository.save(a);
  }

  private WorkflowHistoryEventType createWfEvent(LookupUpsertRequest req, UUID uid) {
    String code = req.code().trim();
    if (workflowHistoryEventTypeRepository.existsByCodeIgnoreCaseAndDeletedAtIsNull(code)) {
      throw new BadRequestException("Duplicate code");
    }
    WorkflowHistoryEventType e = new WorkflowHistoryEventType();
    applySimpleEvent(e, req, uid);
    e.setCreatedBy(uid);
    e.setUpdatedBy(uid);
    return workflowHistoryEventTypeRepository.save(e);
  }

  private WorkflowHistoryEventType updateWfEvent(Long id, LookupUpsertRequest req, UUID uid) {
    WorkflowHistoryEventType e =
        workflowHistoryEventTypeRepository
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Row not found"));
    String code = req.code().trim();
    if (workflowHistoryEventTypeRepository.existsByCodeIgnoreCaseAndIdNotAndDeletedAtIsNull(code, id)) {
      throw new BadRequestException("Duplicate code");
    }
    applySimpleEvent(e, req, uid);
    e.setCode(code);
    e.setUpdatedBy(uid);
    return workflowHistoryEventTypeRepository.save(e);
  }

  private OrgVisualNodeStatus createOrgVisualNode(LookupUpsertRequest req, UUID uid) {
    String code = req.code().trim();
    if (orgVisualNodeStatusRepository.existsByCodeIgnoreCaseAndDeletedAtIsNull(code)) {
      throw new BadRequestException("Duplicate code");
    }
    OrgVisualNodeStatus s = new OrgVisualNodeStatus();
    applyOrgVisualNodeFields(s, req);
    s.setCode(code);
    s.setCreatedBy(uid);
    s.setUpdatedBy(uid);
    return orgVisualNodeStatusRepository.save(s);
  }

  private OrgVisualNodeStatus updateOrgVisualNode(Long id, LookupUpsertRequest req, UUID uid) {
    OrgVisualNodeStatus s =
        orgVisualNodeStatusRepository
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Row not found"));
    String code = req.code().trim();
    if (orgVisualNodeStatusRepository.existsByCodeIgnoreCaseAndIdNotAndDeletedAtIsNull(code, id)) {
      throw new BadRequestException("Duplicate code");
    }
    applyOrgVisualNodeFields(s, req);
    s.setCode(code);
    s.setUpdatedBy(uid);
    return orgVisualNodeStatusRepository.save(s);
  }

  private static void applyOrgVisualNodeFields(OrgVisualNodeStatus s, LookupUpsertRequest req) {
    s.setNameAr(req.nameAr().trim());
    s.setNameEn(req.nameEn().trim());
    s.setDescription(req.description());
    s.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
    s.setActive(req.active() != null ? req.active() : true);
  }

  private static void applyCommon(CorrespondenceType t, LookupUpsertRequest req, UUID uid) {
    t.setNameAr(req.nameAr().trim());
    t.setNameEn(req.nameEn().trim());
    t.setDescription(req.description());
    t.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
    t.setActive(req.active() != null ? req.active() : true);
  }

  private static void applySimpleEvent(WorkflowHistoryEventType e, LookupUpsertRequest req, UUID uid) {
    e.setCode(req.code().trim());
    e.setNameAr(req.nameAr().trim());
    e.setNameEn(req.nameEn().trim());
    e.setDescription(req.description());
    e.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
    e.setActive(req.active() != null ? req.active() : true);
  }

  private static LookupCatalogDto toCatalogDto(LookupCatalog c) {
    return new LookupCatalogDto(
        c.getLookupCode(),
        c.getNameAr(),
        c.getNameEn(),
        c.getParentCatalog() != null ? c.getParentCatalog().getLookupCode() : null,
        c.getSortOrder());
  }

  private static LookupRowAdminDto mapType(CorrespondenceType t, String lookupCode) {
    return new LookupRowAdminDto(
        t.getId(),
        lookupCode,
        t.getCode(),
        t.getNameAr(),
        t.getNameEn(),
        t.getDescription(),
        t.getSortOrder(),
        t.getActive(),
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private static LookupRowAdminDto mapStatus(CorrespondenceStatus s, String lookupCode) {
    return new LookupRowAdminDto(
        s.getId(),
        lookupCode,
        s.getCode(),
        s.getNameAr(),
        s.getNameEn(),
        s.getDescription(),
        s.getSortOrder(),
        s.getActive(),
        s.getCorrespondenceType() != null ? s.getCorrespondenceType().getId() : null,
        s.getTerminal(),
        null,
        null,
        null,
        s.getUiVariant());
  }

  private static LookupRowAdminDto mapPriority(Priority p, String lookupCode) {
    return new LookupRowAdminDto(
        p.getId(),
        lookupCode,
        p.getCode(),
        p.getNameAr(),
        p.getNameEn(),
        p.getDescription(),
        p.getSortOrder(),
        p.getActive(),
        null,
        null,
        p.getSlaDays(),
        null,
        null,
        null);
  }

  private static LookupRowAdminDto mapConf(Confidentiality c, String lookupCode) {
    return new LookupRowAdminDto(
        c.getId(),
        lookupCode,
        c.getCode(),
        c.getNameAr(),
        c.getNameEn(),
        c.getDescription(),
        c.getSortOrder(),
        c.getActive(),
        null,
        null,
        null,
        c.getRestrictsExport(),
        c.getRequiresClearance(),
        null);
  }

  private static LookupRowAdminDto mapClassification(Classification c, String lookupCode) {
    return new LookupRowAdminDto(
        c.getId(),
        lookupCode,
        c.getCode(),
        c.getNameAr(),
        c.getNameEn(),
        c.getDescription(),
        c.getSortOrder(),
        c.getActive(),
        c.getParent() != null ? c.getParent().getId() : null,
        null,
        null,
        null,
        null,
        null);
  }

  private static LookupRowAdminDto mapWfAction(WorkflowActionType a, String lookupCode) {
    return new LookupRowAdminDto(
        a.getId(),
        lookupCode,
        a.getCode(),
        a.getNameAr(),
        a.getNameEn(),
        a.getDescription(),
        a.getSortOrder(),
        a.getActive(),
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private static LookupRowAdminDto mapWfEvent(WorkflowHistoryEventType e, String lookupCode) {
    return new LookupRowAdminDto(
        e.getId(),
        lookupCode,
        e.getCode(),
        e.getNameAr(),
        e.getNameEn(),
        e.getDescription(),
        e.getSortOrder(),
        e.getActive(),
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private static LookupRowAdminDto mapOrgVisualNode(OrgVisualNodeStatus s, String lookupCode) {
    return new LookupRowAdminDto(
        s.getId(),
        lookupCode,
        s.getCode(),
        s.getNameAr(),
        s.getNameEn(),
        s.getDescription(),
        s.getSortOrder(),
        s.getActive(),
        null,
        null,
        null,
        null,
        null,
        null);
  }
}
