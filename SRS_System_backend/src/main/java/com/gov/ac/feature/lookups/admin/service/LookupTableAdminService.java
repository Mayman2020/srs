package com.gov.ac.feature.lookups.admin.service;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.feature.shared.entity.SoftDeletableEntity;
import com.gov.ac.feature.lookups.entity.ConfidentialityEntity;
import com.gov.ac.feature.lookups.entity.CorrespondenceStatusEntity;
import com.gov.ac.feature.lookups.entity.CorrespondenceTypeEntity;
import com.gov.ac.feature.lookups.entity.OrgVisualNodeStatusEntity;
import com.gov.ac.feature.lookups.entity.PriorityEntity;
import com.gov.ac.feature.lookups.entity.WorkflowActionTypeEntity;
import com.gov.ac.feature.lookups.entity.WorkflowHistoryEventTypeEntity;
import com.gov.ac.feature.leave.entity.LeaveStatusEntity;
import com.gov.ac.feature.leave.repository.LeaveStatusRepository;
import com.gov.ac.feature.lookups.entity.ClassificationEntity;
import com.gov.ac.feature.lookups.admin.dto.LookupCatalogDto;
import com.gov.ac.feature.lookups.admin.dto.LookupRowAdminDto;
import com.gov.ac.feature.lookups.admin.dto.LookupUpsertRequestDto;
import com.gov.ac.feature.lookups.admin.mapper.LookupTableAdminMapper;
import com.gov.ac.feature.lookups.repository.ClassificationRepository;
import com.gov.ac.feature.lookups.repository.ConfidentialityRepository;
import com.gov.ac.feature.lookups.repository.CorrespondenceStatusRepository;
import com.gov.ac.feature.lookups.repository.CorrespondenceTypeRepository;
import com.gov.ac.feature.lookups.repository.LookupCatalogRepository;
import com.gov.ac.feature.lookups.repository.OrgVisualNodeStatusRepository;
import com.gov.ac.feature.lookups.repository.PriorityRepository;
import com.gov.ac.feature.lookups.repository.WorkflowActionTypeRepository;
import com.gov.ac.feature.lookups.repository.WorkflowHistoryEventTypeRepository;
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
          "org_visual_node_status",
          "leave_status");

  private final LookupCatalogRepository lookupCatalogRepository;
  private final CorrespondenceTypeRepository correspondenceTypeRepository;
  private final CorrespondenceStatusRepository correspondenceStatusRepository;
  private final PriorityRepository priorityRepository;
  private final ConfidentialityRepository confidentialityRepository;
  private final ClassificationRepository classificationRepository;
  private final WorkflowActionTypeRepository workflowActionTypeRepository;
  private final WorkflowHistoryEventTypeRepository workflowHistoryEventTypeRepository;
  private final OrgVisualNodeStatusRepository orgVisualNodeStatusRepository;
  private final LeaveStatusRepository leaveStatusRepository;

  @Transactional(readOnly = true)
  public List<LookupCatalogDto> listCatalog() {
    return lookupCatalogRepository.findAllByOrderBySortOrderAsc().stream()
        .map(LookupTableAdminMapper::toCatalogDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<LookupRowAdminDto> listRows(String lookupCode) {
    requireManaged(lookupCode);
    return switch (lookupCode) {
      case "correspondence_type" ->
          correspondenceTypeRepository.findByDeletedAtIsNullOrderBySortOrderAsc().stream()
              .map(t -> LookupTableAdminMapper.mapType(t, lookupCode))
              .toList();
      case "correspondence_status" ->
          correspondenceStatusRepository.findByDeletedAtIsNullOrderBySortOrderAsc().stream()
              .map(s -> LookupTableAdminMapper.mapStatus(s, lookupCode))
              .toList();
      case "priority" ->
          priorityRepository.findByDeletedAtIsNullOrderBySortOrderAsc().stream()
              .map(p -> LookupTableAdminMapper.mapPriority(p, lookupCode))
              .toList();
      case "confidentiality" ->
          confidentialityRepository.findByDeletedAtIsNullOrderBySortOrderAsc().stream()
              .map(c -> LookupTableAdminMapper.mapConf(c, lookupCode))
              .toList();
      case "classification" ->
          classificationRepository.findByDeletedAtIsNullOrderBySortOrderAsc().stream()
              .map(c -> LookupTableAdminMapper.mapClassification(c, lookupCode))
              .toList();
      case "workflow_action_type" ->
          workflowActionTypeRepository.findByDeletedAtIsNullOrderBySortOrderAsc().stream()
              .map(a -> LookupTableAdminMapper.mapWfAction(a, lookupCode))
              .toList();
      case "workflow_history_event_type" ->
          workflowHistoryEventTypeRepository.findByDeletedAtIsNullOrderBySortOrderAsc().stream()
              .map(e -> LookupTableAdminMapper.mapWfEvent(e, lookupCode))
              .toList();
      case "org_visual_node_status" ->
          orgVisualNodeStatusRepository.findByDeletedAtIsNullOrderBySortOrderAsc().stream()
              .map(s -> LookupTableAdminMapper.mapOrgVisualNode(s, lookupCode))
              .toList();
      case "leave_status" ->
          leaveStatusRepository.findByDeletedAtIsNullOrderBySortOrderAsc().stream()
              .map(s -> LookupTableAdminMapper.mapLeaveStatus(s, lookupCode))
              .toList();
      default -> throw new NotFoundException("Unknown lookup");
    };
  }

  @Transactional
  public LookupRowAdminDto create(String lookupCode, LookupUpsertRequestDto req) {
    requireManaged(lookupCode);
    UUID uid = SecurityUtils.requireCurrentUserId();
    return switch (lookupCode) {
      case "correspondence_type" -> LookupTableAdminMapper.mapType(createType(req, uid), lookupCode);
      case "correspondence_status" -> LookupTableAdminMapper.mapStatus(createStatus(req, uid), lookupCode);
      case "priority" -> LookupTableAdminMapper.mapPriority(createPriority(req, uid), lookupCode);
      case "confidentiality" -> LookupTableAdminMapper.mapConf(createConf(req, uid), lookupCode);
      case "classification" ->
          LookupTableAdminMapper.mapClassification(createClassification(req, uid), lookupCode);
      case "workflow_action_type" ->
          LookupTableAdminMapper.mapWfAction(createWfAction(req, uid), lookupCode);
      case "workflow_history_event_type" ->
          LookupTableAdminMapper.mapWfEvent(createWfEvent(req, uid), lookupCode);
      case "org_visual_node_status" ->
          LookupTableAdminMapper.mapOrgVisualNode(createOrgVisualNode(req, uid), lookupCode);
      case "leave_status" ->
          LookupTableAdminMapper.mapLeaveStatus(createLeaveStatus(req, uid), lookupCode);
      default -> throw new NotFoundException("Unknown lookup");
    };
  }

  @Transactional
  public LookupRowAdminDto update(String lookupCode, Long id, LookupUpsertRequestDto req) {
    requireManaged(lookupCode);
    UUID uid = SecurityUtils.requireCurrentUserId();
    return switch (lookupCode) {
      case "correspondence_type" -> LookupTableAdminMapper.mapType(updateType(id, req, uid), lookupCode);
      case "correspondence_status" -> LookupTableAdminMapper.mapStatus(updateStatus(id, req, uid), lookupCode);
      case "priority" -> LookupTableAdminMapper.mapPriority(updatePriority(id, req, uid), lookupCode);
      case "confidentiality" -> LookupTableAdminMapper.mapConf(updateConf(id, req, uid), lookupCode);
      case "classification" ->
          LookupTableAdminMapper.mapClassification(updateClassification(id, req, uid), lookupCode);
      case "workflow_action_type" ->
          LookupTableAdminMapper.mapWfAction(updateWfAction(id, req, uid), lookupCode);
      case "workflow_history_event_type" ->
          LookupTableAdminMapper.mapWfEvent(updateWfEvent(id, req, uid), lookupCode);
      case "org_visual_node_status" ->
          LookupTableAdminMapper.mapOrgVisualNode(updateOrgVisualNode(id, req, uid), lookupCode);
      case "leave_status" ->
          LookupTableAdminMapper.mapLeaveStatus(updateLeaveStatus(id, req, uid), lookupCode);
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
        CorrespondenceTypeEntity t =
            correspondenceTypeRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("Row not found"));
        t.setDeletedAt(now);
        t.setDeletedBy(uid);
        t.setUpdatedBy(uid);
      }
      case "correspondence_status" -> {
        CorrespondenceStatusEntity s =
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
      case "leave_status" ->
          markDeleted(
              leaveStatusRepository
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

  private CorrespondenceTypeEntity createType(LookupUpsertRequestDto req, UUID uid) {
    String code = req.code().trim();
    if (correspondenceTypeRepository.existsByCodeIgnoreCaseAndDeletedAtIsNull(code)) {
      throw new BadRequestException("Duplicate code");
    }
    CorrespondenceTypeEntity t = new CorrespondenceTypeEntity();
    applyCommon(t, req, uid);
    t.setCode(code);
    t.setCreatedBy(uid);
    t.setUpdatedBy(uid);
    return correspondenceTypeRepository.save(t);
  }

  private CorrespondenceTypeEntity updateType(Long id, LookupUpsertRequestDto req, UUID uid) {
    CorrespondenceTypeEntity t =
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

  private CorrespondenceStatusEntity createStatus(LookupUpsertRequestDto req, UUID uid) {
    String code = req.code().trim();
    assertStatusCodeUnique(null, req.parentId(), code);
    CorrespondenceStatusEntity s = new CorrespondenceStatusEntity();
    s.setCode(code);
    s.setNameAr(req.nameAr().trim());
    s.setNameEn(req.nameEn().trim());
    s.setDescription(req.description());
    s.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
    s.setActive(req.active() != null ? req.active() : true);
    s.setTerminal(Boolean.TRUE.equals(req.terminal()));
    if (req.parentId() != null) {
      CorrespondenceTypeEntity type =
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

  private CorrespondenceStatusEntity updateStatus(Long id, LookupUpsertRequestDto req, UUID uid) {
    CorrespondenceStatusEntity s =
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
      CorrespondenceTypeEntity type =
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

  private PriorityEntity createPriority(LookupUpsertRequestDto req, UUID uid) {
    String code = req.code().trim();
    if (priorityRepository.existsByCodeIgnoreCaseAndDeletedAtIsNull(code)) {
      throw new BadRequestException("Duplicate code");
    }
    PriorityEntity p = new PriorityEntity();
    p.setCode(code);
    p.setNameAr(req.nameAr().trim());
    p.setNameEn(req.nameEn().trim());
    p.setDescription(req.description());
    p.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
    p.setActive(req.active() != null ? req.active() : true);
    p.setSlaDays(req.slaDays());
    p.setUiVariant(normalizeCorrespondenceStatusUiVariant(req.uiVariant()));
    p.setCreatedBy(uid);
    p.setUpdatedBy(uid);
    return priorityRepository.save(p);
  }

  private PriorityEntity updatePriority(Long id, LookupUpsertRequestDto req, UUID uid) {
    PriorityEntity p =
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
    p.setUiVariant(normalizeCorrespondenceStatusUiVariant(req.uiVariant()));
    p.setUpdatedBy(uid);
    return priorityRepository.save(p);
  }

  private ConfidentialityEntity createConf(LookupUpsertRequestDto req, UUID uid) {
    String code = req.code().trim();
    if (confidentialityRepository.existsByCodeIgnoreCaseAndDeletedAtIsNull(code)) {
      throw new BadRequestException("Duplicate code");
    }
    ConfidentialityEntity c = new ConfidentialityEntity();
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

  private ConfidentialityEntity updateConf(Long id, LookupUpsertRequestDto req, UUID uid) {
    ConfidentialityEntity c =
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

  private ClassificationEntity createClassification(LookupUpsertRequestDto req, UUID uid) {
    String code = req.code().trim();
    assertClassificationCodeUnique(null, req.parentId(), code);
    ClassificationEntity c = new ClassificationEntity();
    c.setCode(code);
    c.setNameAr(req.nameAr().trim());
    c.setNameEn(req.nameEn().trim());
    c.setDescription(req.description());
    c.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
    c.setActive(req.active() != null ? req.active() : true);
    if (req.parentId() != null) {
      ClassificationEntity parent =
          classificationRepository
              .findByIdAndDeletedAtIsNull(req.parentId())
              .orElseThrow(() -> new BadRequestException("Unknown parent classification"));
      c.setParent(parent);
    }
    c.setCreatedBy(uid);
    c.setUpdatedBy(uid);
    return classificationRepository.save(c);
  }

  private ClassificationEntity updateClassification(Long id, LookupUpsertRequestDto req, UUID uid) {
    ClassificationEntity c =
        classificationRepository
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Row not found"));
    String code = req.code().trim();
    assertClassificationCodeUnique(id, req.parentId(), code);
    if (req.parentId() != null && req.parentId().equals(id)) {
      throw new BadRequestException("ClassificationEntity cannot be its own parent");
    }
    c.setCode(code);
    c.setNameAr(req.nameAr().trim());
    c.setNameEn(req.nameEn().trim());
    c.setDescription(req.description());
    c.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
    c.setActive(req.active() != null ? req.active() : true);
    if (req.parentId() != null) {
      ClassificationEntity parent =
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

  private WorkflowActionTypeEntity createWfAction(LookupUpsertRequestDto req, UUID uid) {
    String code = req.code().trim();
    if (workflowActionTypeRepository.existsByCodeIgnoreCaseAndDeletedAtIsNull(code)) {
      throw new BadRequestException("Duplicate code");
    }
    WorkflowActionTypeEntity a = new WorkflowActionTypeEntity();
    a.setCode(code);
    a.setNameAr(req.nameAr().trim());
    a.setNameEn(req.nameEn().trim());
    a.setDescription(req.description());
    a.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
    a.setActive(req.active() != null ? req.active() : true);
    applyWfActionFlags(a, req);
    a.setCreatedBy(uid);
    a.setUpdatedBy(uid);
    return workflowActionTypeRepository.save(a);
  }

  private WorkflowActionTypeEntity updateWfAction(Long id, LookupUpsertRequestDto req, UUID uid) {
    WorkflowActionTypeEntity a =
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
    applyWfActionFlags(a, req);
    a.setUpdatedBy(uid);
    return workflowActionTypeRepository.save(a);
  }

  /** Slice 5 — admin-editable boolean gates on {@code workflow_action_type}. */
  private static void applyWfActionFlags(WorkflowActionTypeEntity a, LookupUpsertRequestDto req) {
    if (req.requiresComment() != null) {
      a.setRequiresComment(req.requiresComment());
    }
    if (req.requiresSignature() != null) {
      a.setRequiresSignature(req.requiresSignature());
    }
  }

  private WorkflowHistoryEventTypeEntity createWfEvent(LookupUpsertRequestDto req, UUID uid) {
    String code = req.code().trim();
    if (workflowHistoryEventTypeRepository.existsByCodeIgnoreCaseAndDeletedAtIsNull(code)) {
      throw new BadRequestException("Duplicate code");
    }
    WorkflowHistoryEventTypeEntity e = new WorkflowHistoryEventTypeEntity();
    applySimpleEvent(e, req, uid);
    e.setCreatedBy(uid);
    e.setUpdatedBy(uid);
    return workflowHistoryEventTypeRepository.save(e);
  }

  private WorkflowHistoryEventTypeEntity updateWfEvent(Long id, LookupUpsertRequestDto req, UUID uid) {
    WorkflowHistoryEventTypeEntity e =
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

  private OrgVisualNodeStatusEntity createOrgVisualNode(LookupUpsertRequestDto req, UUID uid) {
    String code = req.code().trim();
    if (orgVisualNodeStatusRepository.existsByCodeIgnoreCaseAndDeletedAtIsNull(code)) {
      throw new BadRequestException("Duplicate code");
    }
    OrgVisualNodeStatusEntity s = new OrgVisualNodeStatusEntity();
    applyOrgVisualNodeFields(s, req);
    s.setCode(code);
    s.setCreatedBy(uid);
    s.setUpdatedBy(uid);
    return orgVisualNodeStatusRepository.save(s);
  }

  private OrgVisualNodeStatusEntity updateOrgVisualNode(Long id, LookupUpsertRequestDto req, UUID uid) {
    OrgVisualNodeStatusEntity s =
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

  private static void applyOrgVisualNodeFields(OrgVisualNodeStatusEntity s, LookupUpsertRequestDto req) {
    s.setNameAr(req.nameAr().trim());
    s.setNameEn(req.nameEn().trim());
    s.setDescription(req.description());
    s.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
    s.setActive(req.active() != null ? req.active() : true);
  }

  private LeaveStatusEntity createLeaveStatus(LookupUpsertRequestDto req, UUID uid) {
    String code = req.code().trim();
    if (leaveStatusRepository.existsByCodeIgnoreCaseAndDeletedAtIsNull(code)) {
      throw new BadRequestException("Duplicate code");
    }
    LeaveStatusEntity s = new LeaveStatusEntity();
    s.setCode(code);
    applyLeaveStatusFields(s, req);
    if (Boolean.TRUE.equals(s.getInitial())) {
      clearOtherLeaveInitials(null);
    }
    s.setCreatedBy(uid);
    s.setUpdatedBy(uid);
    return leaveStatusRepository.save(s);
  }

  private LeaveStatusEntity updateLeaveStatus(Long id, LookupUpsertRequestDto req, UUID uid) {
    LeaveStatusEntity s =
        leaveStatusRepository
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Row not found"));
    String code = req.code().trim();
    if (leaveStatusRepository.existsByCodeIgnoreCaseAndIdNotAndDeletedAtIsNull(code, id)) {
      throw new BadRequestException("Duplicate code");
    }
    s.setCode(code);
    applyLeaveStatusFields(s, req);
    if (Boolean.TRUE.equals(s.getInitial())) {
      clearOtherLeaveInitials(id);
    }
    s.setUpdatedBy(uid);
    return leaveStatusRepository.save(s);
  }

  private static void applyLeaveStatusFields(LeaveStatusEntity s, LookupUpsertRequestDto req) {
    s.setNameAr(req.nameAr().trim());
    s.setNameEn(req.nameEn().trim());
    s.setDescription(req.description());
    s.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
    s.setActive(req.active() != null ? req.active() : true);
    s.setTerminal(req.terminal() != null ? req.terminal() : false);
    s.setInitial(req.initial() != null ? req.initial() : false);
    s.setUiVariant(normalizeCorrespondenceStatusUiVariant(req.uiVariant()));
  }

  private void clearOtherLeaveInitials(Long keepId) {
    for (LeaveStatusEntity row : leaveStatusRepository.findByDeletedAtIsNullOrderBySortOrderAsc()) {
      if (keepId != null && keepId.equals(row.getId())) {
        continue;
      }
      if (Boolean.TRUE.equals(row.getInitial())) {
        row.setInitial(false);
      }
    }
  }

  private static void applyCommon(CorrespondenceTypeEntity t, LookupUpsertRequestDto req, UUID uid) {
    t.setNameAr(req.nameAr().trim());
    t.setNameEn(req.nameEn().trim());
    t.setDescription(req.description());
    t.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
    t.setActive(req.active() != null ? req.active() : true);
  }

  private static void applySimpleEvent(WorkflowHistoryEventTypeEntity e, LookupUpsertRequestDto req, UUID uid) {
    e.setCode(req.code().trim());
    e.setNameAr(req.nameAr().trim());
    e.setNameEn(req.nameEn().trim());
    e.setDescription(req.description());
    e.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
    e.setActive(req.active() != null ? req.active() : true);
  }

}
