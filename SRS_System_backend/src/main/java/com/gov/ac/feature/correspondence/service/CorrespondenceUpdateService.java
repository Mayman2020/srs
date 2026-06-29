package com.gov.ac.feature.correspondence.service;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.ForbiddenException;
import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.feature.correspondence.audit.CorrespondenceActionAudit;
import com.gov.ac.feature.correspondence.dto.CorrespondenceDetailResponseDto;
import com.gov.ac.feature.correspondence.dto.CorrespondencePatchRequestDto;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.correspondence.security.CorrespondenceViewAuthorization;
import com.gov.ac.feature.departments.entity.DepartmentEntity;
import com.gov.ac.feature.departments.repository.DepartmentRepository;
import com.gov.ac.feature.organizations.entity.OrganizationEntity;
import com.gov.ac.feature.organizations.repository.OrganizationRepository;
import com.gov.ac.feature.shared.lookup.service.LookupResolutionService;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CorrespondenceUpdateService {

  public static final String ACTION_UPDATED = "CORRESPONDENCE_UPDATED";

  private final CorrespondenceRepository correspondenceRepository;
  private final AppUserRepository appUserRepository;
  private final OrganizationRepository organizationRepository;
  private final DepartmentRepository departmentRepository;
  private final CorrespondenceViewAuthorization correspondenceViewAuthorization;
  private final LookupResolutionService lookups;
  private final CorrespondenceDetailService correspondenceDetailService;
  private final CorrespondenceActionAudit correspondenceActionAudit;

  @Transactional
  public CorrespondenceDetailResponseDto patch(
      UUID correspondenceId, UUID actorUserId, CorrespondencePatchRequestDto body) {
    CorrespondenceEntity c = loadMutable(correspondenceId, actorUserId);
    Map<String, Object> changed = new HashMap<>();

    if (StringUtils.hasText(body.subject())) {
      String v = body.subject().trim();
      changed.put("subject", v);
      c.setSubject(v);
    }
    if (body.description() != null) {
      changed.put("description", body.description());
      c.setDescription(trimToNull(body.description()));
    }
    if (body.bodyHtml() != null) {
      changed.put("bodyHtml", true);
      c.setBodyHtml(body.bodyHtml());
    }
    if (StringUtils.hasText(body.priorityCode())) {
      var priority = lookups.requireActivePriority(body.priorityCode().trim());
      changed.put("priorityCode", priority.getCode());
      c.setPriority(priority);
    }
    if (StringUtils.hasText(body.confidentialityCode())) {
      var conf = lookups.requireActiveConfidentiality(body.confidentialityCode().trim());
      changed.put("confidentialityCode", conf.getCode());
      c.setConfidentiality(conf);
    }
    if (StringUtils.hasText(body.classificationCode())) {
      var cls = lookups.requireActiveClassification(body.classificationCode().trim());
      changed.put("classificationCode", cls.getCode());
      c.setClassification(cls);
    }
    if (body.senderOrganizationId() != null) {
      OrganizationEntity org = resolveOrganization(body.senderOrganizationId());
      changed.put("senderOrganizationId", body.senderOrganizationId());
      c.setSenderOrganization(org);
    }
    if (body.recipientOrganizationId() != null) {
      OrganizationEntity org = resolveOrganization(body.recipientOrganizationId());
      changed.put("recipientOrganizationId", body.recipientOrganizationId());
      c.setRecipientOrganization(org);
    }
    if (body.externalReferenceNumber() != null) {
      changed.put("externalReferenceNumber", body.externalReferenceNumber());
      c.setExternalReferenceNumber(trimToNull(body.externalReferenceNumber()));
    }
    if (body.externalReferenceDate() != null) {
      changed.put("externalReferenceDate", body.externalReferenceDate());
      c.setExternalReferenceDate(body.externalReferenceDate());
    }
    if (body.ownerDepartmentId() != null) {
      DepartmentEntity dept = resolveDepartment(body.ownerDepartmentId());
      changed.put("ownerDepartmentId", body.ownerDepartmentId());
      c.setOwnerDepartment(dept);
    }
    if (body.dueDate() != null) {
      changed.put("dueDate", body.dueDate());
      c.setDueDate(body.dueDate());
    }
    if (body.barcodeValue() != null) {
      changed.put("barcodeValue", body.barcodeValue());
      c.setBarcodeValue(trimToNull(body.barcodeValue()));
    }
    if (body.beneficiaryName() != null) {
      changed.put("beneficiaryName", body.beneficiaryName());
      c.setBeneficiaryName(trimToNull(body.beneficiaryName()));
    }
    if (body.beneficiaryOrganization() != null) {
      changed.put("beneficiaryOrganization", body.beneficiaryOrganization());
      c.setBeneficiaryOrganization(trimToNull(body.beneficiaryOrganization()));
    }
    if (body.beneficiaryIdentifier() != null) {
      changed.put("beneficiaryIdentifier", body.beneficiaryIdentifier());
      c.setBeneficiaryIdentifier(trimToNull(body.beneficiaryIdentifier()));
    }

    if (changed.isEmpty()) {
      throw new BadRequestException("No updatable fields were provided");
    }

    c.setUpdatedBy(actorUserId);
    correspondenceRepository.save(c);
    correspondenceActionAudit.log(actorUserId, ACTION_UPDATED, correspondenceId, changed);
    return correspondenceDetailService.getById(correspondenceId, actorUserId);
  }

  private CorrespondenceEntity loadMutable(UUID correspondenceId, UUID actorUserId) {
    AppUserEntity actor =
        appUserRepository
            .findByIdAndDeletedAtIsNull(actorUserId)
            .orElseThrow(() -> new ForbiddenException("You cannot edit this correspondence"));
    if (!Boolean.TRUE.equals(actor.getActive())) {
      throw new ForbiddenException("You cannot edit this correspondence");
    }
    CorrespondenceEntity c =
        correspondenceRepository
            .findByIdAndDeletedAtIsNullWithOwnerDepartment(correspondenceId)
            .orElseThrow(() -> new NotFoundException("CorrespondenceEntity not found"));
    correspondenceViewAuthorization.assertCanView(actor, c);
    CorrespondenceMutationGuards.assertCorrespondenceMutable(c);
    return c;
  }

  private OrganizationEntity resolveOrganization(Long id) {
    if (id == null) {
      return null;
    }
    return organizationRepository
        .findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new BadRequestException("Unknown or deleted organization"));
  }

  private DepartmentEntity resolveDepartment(Long id) {
    if (id == null) {
      return null;
    }
    return departmentRepository
        .findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new BadRequestException("Unknown or deleted department"));
  }

  private static String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }
}
