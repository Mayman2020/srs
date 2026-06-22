package com.gov.ac.feature.sla.service;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.feature.lookups.repository.ConfidentialityRepository;
import com.gov.ac.feature.lookups.repository.CorrespondenceTypeRepository;
import com.gov.ac.feature.lookups.repository.PriorityRepository;
import com.gov.ac.feature.lookups.repository.WorkflowActionTypeRepository;
import com.gov.ac.feature.organization.repository.OrganizationalUnitLevelRepository;
import com.gov.ac.feature.sla.dto.CreateSlaEscalationStepRequestDto;
import com.gov.ac.feature.sla.dto.CreateSlaPolicyRequestDto;
import com.gov.ac.feature.sla.dto.SlaEscalationActionTypeDto;
import com.gov.ac.feature.sla.dto.SlaPolicyDto;
import com.gov.ac.feature.sla.entity.SlaEscalationStepEntity;
import com.gov.ac.feature.sla.entity.SlaPolicyEntity;
import com.gov.ac.feature.sla.mapper.SlaPolicyMapper;
import com.gov.ac.feature.sla.repository.SlaEscalationActionTypeRepository;
import com.gov.ac.feature.sla.repository.SlaEscalationStepRepository;
import com.gov.ac.feature.sla.repository.SlaPolicyRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin CRUD for SLA policies. Validates code uniqueness, lookup ids, escalation action codes,
 * and step ordering on every mutate. Soft-delete keeps historical {@code sla_breach_event} rows
 * pointing at meaningful policy data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SlaPolicyManagementService {

  private final SlaPolicyRepository slaPolicyRepository;
  private final SlaEscalationStepRepository slaEscalationStepRepository;
  private final SlaEscalationActionTypeRepository slaEscalationActionTypeRepository;
  private final CorrespondenceTypeRepository correspondenceTypeRepository;
  private final PriorityRepository priorityRepository;
  private final ConfidentialityRepository confidentialityRepository;
  private final WorkflowActionTypeRepository workflowActionTypeRepository;
  private final OrganizationalUnitLevelRepository organizationalUnitLevelRepository;

  @Transactional(readOnly = true)
  public List<SlaEscalationActionTypeDto> listEscalationActionTypes() {
    return slaEscalationActionTypeRepository.findByActiveTrueOrderBySortOrderAsc().stream()
        .map(
            row ->
                new SlaEscalationActionTypeDto(
                    row.getCode(), row.getNameAr(), row.getNameEn(), row.getSortOrder()))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<SlaPolicyDto> list() {
    return slaPolicyRepository.findByDeletedAtIsNullOrderByIdAsc().stream()
        .map(p -> SlaPolicyMapper.toDto(p, loadSteps(p.getId())))
        .toList();
  }

  @Transactional(readOnly = true)
  public SlaPolicyDto get(Long id) {
    SlaPolicyEntity entity = requireExisting(id);
    return SlaPolicyMapper.toDto(entity, loadSteps(entity.getId()));
  }

  @Transactional
  public SlaPolicyDto create(CreateSlaPolicyRequestDto req, UUID actor) {
    validateLookups(req);
    String code = req.code().trim();
    slaPolicyRepository
        .findByCodeIgnoreCaseAndDeletedAtIsNull(code)
        .ifPresent(
            existing -> {
              throw new BadRequestException("SLA policy code already exists: " + code);
            });
    SlaPolicyEntity entity = new SlaPolicyEntity();
    applyFields(entity, req);
    entity.setCode(code);
    entity.setActive(req.active() == null || req.active());
    entity.setCreatedBy(actor);
    entity.setUpdatedBy(actor);
    SlaPolicyEntity saved = slaPolicyRepository.save(entity);
    replaceSteps(saved, req.steps(), actor);
    log.info("[SLA] policy created id={} code={}", saved.getId(), saved.getCode());
    return SlaPolicyMapper.toDto(saved, loadSteps(saved.getId()));
  }

  @Transactional
  public SlaPolicyDto update(Long id, CreateSlaPolicyRequestDto req, UUID actor) {
    validateLookups(req);
    SlaPolicyEntity entity = requireExisting(id);
    String code = req.code().trim();
    if (!entity.getCode().equalsIgnoreCase(code)) {
      slaPolicyRepository
          .findByCodeIgnoreCaseAndDeletedAtIsNull(code)
          .filter(existing -> !existing.getId().equals(id))
          .ifPresent(
              existing -> {
                throw new BadRequestException("SLA policy code already exists: " + code);
              });
    }
    applyFields(entity, req);
    entity.setCode(code);
    if (req.active() != null) {
      entity.setActive(req.active());
    }
    entity.setUpdatedBy(actor);
    slaPolicyRepository.save(entity);
    replaceSteps(entity, req.steps(), actor);
    log.info("[SLA] policy updated id={} code={}", entity.getId(), entity.getCode());
    return SlaPolicyMapper.toDto(entity, loadSteps(entity.getId()));
  }

  /** Soft-delete a policy. Existing {@code sla_breach_event} rows keep their reference. */
  @Transactional
  public void delete(Long id, UUID actor) {
    SlaPolicyEntity entity = requireExisting(id);
    Instant now = Instant.now();
    entity.setDeletedAt(now);
    entity.setDeletedBy(actor);
    entity.setActive(false);
    slaPolicyRepository.save(entity);
    List<SlaEscalationStepEntity> steps =
        slaEscalationStepRepository.findByPolicy_IdAndDeletedAtIsNullOrderByStepOrderAsc(id);
    for (SlaEscalationStepEntity step : steps) {
      step.setDeletedAt(now);
      step.setDeletedBy(actor);
      step.setActive(false);
      slaEscalationStepRepository.save(step);
    }
    log.info("[SLA] policy soft-deleted id={} code={}", entity.getId(), entity.getCode());
  }

  // ---------------------------------------------------------------------------
  // helpers
  // ---------------------------------------------------------------------------

  private SlaPolicyEntity requireExisting(Long id) {
    if (id == null) {
      throw new BadRequestException("SLA policy id is required");
    }
    return slaPolicyRepository
        .findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new NotFoundException("SLA policy not found: " + id));
  }

  private void applyFields(SlaPolicyEntity entity, CreateSlaPolicyRequestDto req) {
    entity.setNameAr(req.nameAr().trim());
    entity.setNameEn(req.nameEn().trim());
    entity.setDescription(trimToNull(req.description()));
    entity.setCorrespondenceType(
        req.correspondenceTypeId() == null
            ? null
            : correspondenceTypeRepository
                .findByIdAndDeletedAtIsNull(req.correspondenceTypeId())
                .orElseThrow(
                    () -> new BadRequestException(
                        "Unknown correspondence type id: " + req.correspondenceTypeId())));
    entity.setPriority(
        req.priorityId() == null
            ? null
            : priorityRepository
                .findByIdAndDeletedAtIsNull(req.priorityId())
                .orElseThrow(
                    () -> new BadRequestException("Unknown priority id: " + req.priorityId())));
    entity.setConfidentiality(
        req.confidentialityId() == null
            ? null
            : confidentialityRepository
                .findByIdAndDeletedAtIsNull(req.confidentialityId())
                .orElseThrow(
                    () -> new BadRequestException(
                        "Unknown confidentiality id: " + req.confidentialityId())));
    entity.setWorkflowActionType(
        req.workflowActionTypeId() == null
            ? null
            : workflowActionTypeRepository
                .findByIdAndDeletedAtIsNull(req.workflowActionTypeId())
                .orElseThrow(
                    () -> new BadRequestException(
                        "Unknown workflow action type id: " + req.workflowActionTypeId())));
    entity.setOrgLevelCode(trimToNull(req.orgLevelCode()));
    entity.setTargetHours(req.targetHours());
    entity.setBreachGraceMinutes(req.breachGraceMinutes() == null ? 0 : req.breachGraceMinutes());
  }

  private void validateLookups(CreateSlaPolicyRequestDto req) {
    if (req.targetHours() == null || req.targetHours() <= 0) {
      throw new BadRequestException("targetHours must be positive");
    }
    if (req.orgLevelCode() != null && !req.orgLevelCode().isBlank()) {
      organizationalUnitLevelRepository
          .findActiveByCode(req.orgLevelCode())
          .orElseThrow(
              () -> new BadRequestException("Unknown org level code: " + req.orgLevelCode()));
    }
    if (req.steps() != null) {
      Set<Integer> seenOrders = new HashSet<>();
      for (CreateSlaEscalationStepRequestDto step : req.steps()) {
        if (!slaEscalationActionTypeRepository.existsById(step.actionCode())) {
          throw new BadRequestException("Unknown escalation action code: " + step.actionCode());
        }
        if (!seenOrders.add(step.stepOrder())) {
          throw new BadRequestException(
              "Duplicate stepOrder in escalation steps: " + step.stepOrder());
        }
      }
    }
  }

  private void replaceSteps(
      SlaPolicyEntity policy,
      List<CreateSlaEscalationStepRequestDto> requested,
      UUID actor) {
    Instant now = Instant.now();
    List<SlaEscalationStepEntity> existing =
        slaEscalationStepRepository.findByPolicy_IdAndDeletedAtIsNullOrderByStepOrderAsc(
            policy.getId());
    for (SlaEscalationStepEntity row : existing) {
      row.setDeletedAt(now);
      row.setDeletedBy(actor);
      row.setActive(false);
      slaEscalationStepRepository.save(row);
    }
    if (requested == null) {
      return;
    }
    for (CreateSlaEscalationStepRequestDto req : requested) {
      SlaEscalationStepEntity step = new SlaEscalationStepEntity();
      step.setPolicy(policy);
      step.setStepOrder(req.stepOrder());
      step.setActionCode(req.actionCode());
      step.setDelayAfterBreachMinutes(
          req.delayAfterBreachMinutes() == null ? 0 : req.delayAfterBreachMinutes());
      step.setTargetRoleCode(trimToNull(req.targetRoleCode()));
      step.setDescription(trimToNull(req.description()));
      step.setActive(req.active() == null || req.active());
      step.setCreatedBy(actor);
      step.setUpdatedBy(actor);
      slaEscalationStepRepository.save(step);
    }
  }

  private List<SlaEscalationStepEntity> loadSteps(Long policyId) {
    return slaEscalationStepRepository
        .findByPolicy_IdAndDeletedAtIsNullOrderByStepOrderAsc(policyId);
  }

  private static String trimToNull(String raw) {
    if (raw == null) {
      return null;
    }
    String trimmed = raw.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
