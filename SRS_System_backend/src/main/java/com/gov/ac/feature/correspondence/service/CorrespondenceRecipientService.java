package com.gov.ac.feature.correspondence.service;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.ForbiddenException;
import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.feature.correspondence.dto.CorrespondenceRecipientDto;
import com.gov.ac.feature.correspondence.dto.CorrespondenceUserRecipientDto;
import com.gov.ac.feature.correspondence.dto.UpsertCorrespondenceRecipientRequestDto;
import com.gov.ac.feature.correspondence.dto.UpsertCorrespondenceUserRecipientRequestDto;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.correspondence.entity.CorrespondenceRecipientEntity;
import com.gov.ac.feature.correspondence.entity.CorrespondenceRecipientKindEntity;
import com.gov.ac.feature.correspondence.entity.CorrespondenceUserRecipientEntity;
import com.gov.ac.feature.correspondence.security.CorrespondenceViewAuthorization;
import com.gov.ac.feature.departments.entity.DepartmentEntity;
import com.gov.ac.feature.departments.repository.DepartmentRepository;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRecipientKindRepository;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRecipientRepository;
import com.gov.ac.feature.correspondence.repository.CorrespondenceUserRecipientRepository;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CorrespondenceRecipientService {

  private final CorrespondenceRepository correspondenceRepository;
  private final CorrespondenceRecipientRepository correspondenceRecipientRepository;
  private final CorrespondenceUserRecipientRepository correspondenceUserRecipientRepository;
  private final CorrespondenceRecipientKindRepository correspondenceRecipientKindRepository;
  private final DepartmentRepository departmentRepository;
  private final AppUserRepository appUserRepository;
  private final CorrespondenceViewAuthorization correspondenceViewAuthorization;

  @Transactional(readOnly = true)
  public List<CorrespondenceRecipientDto> list(UUID correspondenceId, UUID viewerId) {
    assertCorrespondenceView(correspondenceId, viewerId);
    return correspondenceRecipientRepository.listActiveForCorrespondence(correspondenceId).stream()
        .map(this::toDto)
        .toList();
  }

  @Transactional
  public CorrespondenceRecipientDto add(
      UUID correspondenceId, UUID viewerId, UpsertCorrespondenceRecipientRequestDto req) {
    CorrespondenceEntity base = assertCorrespondenceMutable(correspondenceId, viewerId);
    if (correspondenceRecipientRepository.existsActivePair(correspondenceId, req.departmentId())) {
      throw new BadRequestException("This department is already a recipient");
    }
    DepartmentEntity dept =
        departmentRepository
            .findByIdAndDeletedAtIsNull(req.departmentId())
            .orElseThrow(() -> new BadRequestException("Unknown or deleted department"));
    CorrespondenceRecipientEntity row = new CorrespondenceRecipientEntity();
    row.setCorrespondence(base);
    row.setDepartment(dept);
    row.setCreatedBy(viewerId);
    row.setUpdatedBy(viewerId);
    CorrespondenceRecipientEntity saved = correspondenceRecipientRepository.save(row);
    saved.setDepartment(dept);
    return toDto(saved);
  }

  @Transactional
  public void delete(UUID correspondenceId, UUID viewerId, long recipientId) {
    assertCorrespondenceMutable(correspondenceId, viewerId);
    CorrespondenceRecipientEntity row =
        correspondenceRecipientRepository
            .findActiveByIdAndCorrespondence(recipientId, correspondenceId)
            .orElseThrow(() -> new NotFoundException("Recipient not found"));
    row.setDeletedAt(Instant.now());
    row.setDeletedBy(viewerId);
  }

  @Transactional(readOnly = true)
  public List<CorrespondenceUserRecipientDto> listUserRecipients(UUID correspondenceId, UUID viewerId) {
    assertCorrespondenceView(correspondenceId, viewerId);
    return correspondenceUserRecipientRepository.listActiveForCorrespondence(correspondenceId).stream()
        .map(this::toUserDto)
        .toList();
  }

  @Transactional
  public CorrespondenceUserRecipientDto addUserRecipient(
      UUID correspondenceId, UUID viewerId, UpsertCorrespondenceUserRecipientRequestDto req) {
    CorrespondenceEntity base = assertCorrespondenceMutable(correspondenceId, viewerId);
    CorrespondenceRecipientKindEntity kind =
        correspondenceRecipientKindRepository
            .findActiveByCode(req.recipientKindCode().trim())
            .orElseThrow(() -> new BadRequestException("Unknown recipient kind"));
    AppUserEntity recipient =
        appUserRepository
            .findByIdAndDeletedAtIsNull(req.recipientUserId())
            .filter(u -> Boolean.TRUE.equals(u.getActive()))
            .orElseThrow(() -> new BadRequestException("Unknown or inactive user"));
    if (correspondenceUserRecipientRepository.existsActiveTriple(
        correspondenceId, req.recipientUserId(), kind.getId())) {
      throw new BadRequestException("This user is already a recipient for this kind");
    }
    CorrespondenceUserRecipientEntity row = new CorrespondenceUserRecipientEntity();
    row.setCorrespondence(base);
    row.setRecipientUser(recipient);
    row.setRecipientKind(kind);
    row.setCreatedBy(viewerId);
    row.setUpdatedBy(viewerId);
    CorrespondenceUserRecipientEntity saved = correspondenceUserRecipientRepository.save(row);
    saved.setRecipientUser(recipient);
    saved.setRecipientKind(kind);
    return toUserDto(saved);
  }

  @Transactional
  public void deleteUserRecipient(UUID correspondenceId, UUID viewerId, long recipientId) {
    assertCorrespondenceMutable(correspondenceId, viewerId);
    CorrespondenceUserRecipientEntity row =
        correspondenceUserRecipientRepository
            .findActiveByIdAndCorrespondence(recipientId, correspondenceId)
            .orElseThrow(() -> new NotFoundException("User recipient not found"));
    row.setDeletedAt(Instant.now());
    row.setDeletedBy(viewerId);
  }

  private CorrespondenceEntity assertCorrespondenceView(UUID correspondenceId, UUID viewerId) {
    CorrespondenceEntity c =
        correspondenceRepository
            .findDetailGraphByIdAndDeletedAtIsNull(correspondenceId)
            .orElseThrow(() -> new NotFoundException("CorrespondenceEntity not found"));
    AppUserEntity viewer = loadActiveViewer(viewerId);
    correspondenceViewAuthorization.assertCanView(viewer, c);
    return c;
  }

  private CorrespondenceEntity assertCorrespondenceMutable(UUID correspondenceId, UUID viewerId) {
    CorrespondenceEntity c = assertCorrespondenceView(correspondenceId, viewerId);
    CorrespondenceMutationGuards.assertCorrespondenceMutable(c);
    return c;
  }

  private AppUserEntity loadActiveViewer(UUID viewerId) {
    AppUserEntity viewer =
        appUserRepository
            .findByIdAndDeletedAtIsNull(viewerId)
            .orElseThrow(
                () -> {
                  log.warn("Recipient denied: unknown viewer userId={}", viewerId);
                  return new ForbiddenException("You do not have access to this correspondence");
                });
    if (!Boolean.TRUE.equals(viewer.getActive())) {
      throw new ForbiddenException("You do not have access to this correspondence");
    }
    return viewer;
  }

  private CorrespondenceRecipientDto toDto(CorrespondenceRecipientEntity row) {
    DepartmentEntity d = row.getDepartment();
    return new CorrespondenceRecipientDto(
        row.getId(),
        d.getId(),
        d.getCode(),
        d.getNameAr(),
        d.getNameEn(),
        row.getFirstReadAt(),
        row.getLastReadAt(),
        row.getReadCount());
  }

  private CorrespondenceUserRecipientDto toUserDto(CorrespondenceUserRecipientEntity row) {
    AppUserEntity u = row.getRecipientUser();
    return new CorrespondenceUserRecipientDto(
        row.getId(),
        u.getId().toString(),
        u.getUsername(),
        u.getFullNameAr(),
        u.getFullNameEn(),
        row.getRecipientKind().getCode(),
        row.getFirstReadAt(),
        row.getLastReadAt(),
        row.getReadCount(),
        row.getAcknowledgedAt());
  }
}
