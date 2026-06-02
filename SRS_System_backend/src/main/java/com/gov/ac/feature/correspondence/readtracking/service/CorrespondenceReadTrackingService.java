package com.gov.ac.feature.correspondence.readtracking.service;

import com.gov.ac.common.api.ForbiddenException;
import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.feature.audit.dto.CreateAuditEventRequestDto;
import com.gov.ac.feature.audit.service.AuditTrailService;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.correspondence.readtracking.dto.CorrespondenceReadReceiptDto;
import com.gov.ac.feature.correspondence.readtracking.dto.CorrespondenceReadStatusSummaryDto;
import com.gov.ac.feature.correspondence.readtracking.entity.CorrespondenceReadReceiptEntity;
import com.gov.ac.feature.correspondence.readtracking.repository.CorrespondenceReadReceiptRepository;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import com.gov.ac.feature.correspondence.security.CorrespondenceViewAuthorization;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read tracking for {@link CorrespondenceEntity}.
 *
 * <p>Writes use {@link Propagation#REQUIRES_NEW} so that a tracking failure cannot poison the
 * outer detail view's read-only transaction. Callers wrap invocations in a try/catch and log
 * (see {@code CorrespondenceDetailService} / {@code CorrespondenceReadStatusController}).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CorrespondenceReadTrackingService {

  static final String ACTION_FIRST_OPENED = "CORRESPONDENCE_FIRST_OPENED";
  static final String ACTION_ACKED = "CORRESPONDENCE_ACKED";
  private static final String RESOURCE_TYPE = "CORRESPONDENCE";

  private final CorrespondenceReadReceiptRepository readReceiptRepository;
  private final CorrespondenceRepository correspondenceRepository;
  private final AppUserRepository appUserRepository;
  private final CorrespondenceViewAuthorization correspondenceViewAuthorization;
  private final AuditTrailService auditTrailService;

  /**
   * Record an authorized open. First call inserts a row and emits {@code
   * CORRESPONDENCE_FIRST_OPENED}; subsequent calls only bump {@code openCount} / {@code
   * lastOpenedAt}.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordOpen(UUID correspondenceId, UUID userId) {
    if (correspondenceId == null || userId == null) {
      return;
    }
    Instant now = Instant.now();
    Optional<CorrespondenceReadReceiptEntity> existing =
        readReceiptRepository.findActiveByCorrespondenceAndUser(correspondenceId, userId);
    if (existing.isPresent()) {
      CorrespondenceReadReceiptEntity row = existing.get();
      row.setLastOpenedAt(now);
      row.setOpenCount(row.getOpenCount() + 1);
      readReceiptRepository.save(row);
      return;
    }

    CorrespondenceEntity correspondence =
        correspondenceRepository
            .findById(correspondenceId)
            .filter(c -> c.getDeletedAt() == null)
            .orElse(null);
    AppUserEntity user =
        appUserRepository.findByIdAndDeletedAtIsNull(userId).orElse(null);
    if (correspondence == null || user == null) {
      return;
    }
    CorrespondenceReadReceiptEntity row = new CorrespondenceReadReceiptEntity();
    row.setCorrespondence(correspondence);
    row.setUser(user);
    row.setFirstOpenedAt(now);
    row.setLastOpenedAt(now);
    row.setOpenCount(1);
    readReceiptRepository.save(row);

    auditTrailService.append(
        new CreateAuditEventRequestDto(
            userId.toString(),
            ACTION_FIRST_OPENED,
            RESOURCE_TYPE,
            correspondenceId.toString(),
            null,
            null,
            null,
            now));
  }

  /**
   * Idempotent acknowledge. Loads the correspondence and viewer, re-runs the view authorization
   * check, ensures a receipt exists, and only audits the first transition from {@code null} to
   * a non-null {@code acknowledgedAt}.
   */
  @Transactional
  public CorrespondenceReadReceiptDto acknowledge(
      UUID correspondenceId, UUID userId, String comment) {
    CorrespondenceEntity correspondence =
        correspondenceRepository
            .findById(correspondenceId)
            .filter(c -> c.getDeletedAt() == null)
            .orElseThrow(() -> new NotFoundException("CorrespondenceEntity not found"));
    AppUserEntity viewer =
        appUserRepository
            .findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new ForbiddenException("You do not have access to this correspondence"));
    if (!Boolean.TRUE.equals(viewer.getActive())) {
      throw new ForbiddenException("You do not have access to this correspondence");
    }
    correspondenceViewAuthorization.assertCanView(viewer, correspondence);

    Instant now = Instant.now();
    CorrespondenceReadReceiptEntity row =
        readReceiptRepository
            .findActiveByCorrespondenceAndUser(correspondenceId, userId)
            .orElseGet(
                () -> {
                  CorrespondenceReadReceiptEntity r = new CorrespondenceReadReceiptEntity();
                  r.setCorrespondence(correspondence);
                  r.setUser(viewer);
                  r.setFirstOpenedAt(now);
                  r.setLastOpenedAt(now);
                  r.setOpenCount(1);
                  return r;
                });

    boolean firstAck = row.getAcknowledgedAt() == null;
    if (firstAck) {
      row.setAcknowledgedAt(now);
    }
    if (comment != null && !comment.isBlank()) {
      row.setAcknowledgementComment(comment);
    }
    CorrespondenceReadReceiptEntity saved = readReceiptRepository.save(row);

    if (firstAck) {
      auditTrailService.append(
          new CreateAuditEventRequestDto(
              userId.toString(),
              ACTION_ACKED,
              RESOURCE_TYPE,
              correspondenceId.toString(),
              null,
              null,
              null,
              now));
    }

    return toOwnDto(saved);
  }

  /** Own receipt, suitable for embedding in the detail response (no user identity fields). */
  @Transactional(readOnly = true)
  public Optional<CorrespondenceReadReceiptDto> getOwnReceipt(
      UUID correspondenceId, UUID userId) {
    return readReceiptRepository
        .findActiveByCorrespondenceAndUser(correspondenceId, userId)
        .map(this::toOwnDto);
  }

  /** Cross-user read status; caller must hold {@code CORRESPONDENCE_READ_STATUS_VIEW}. */
  @Transactional(readOnly = true)
  public CorrespondenceReadStatusSummaryDto listForCorrespondence(UUID correspondenceId) {
    List<CorrespondenceReadReceiptEntity> rows =
        readReceiptRepository.findAllActiveByCorrespondence(correspondenceId);
    List<CorrespondenceReadReceiptDto> dtos = rows.stream().map(this::toFullDto).toList();
    int acknowledged = (int) dtos.stream().filter(r -> r.acknowledgedAt() != null).count();
    return new CorrespondenceReadStatusSummaryDto(
        correspondenceId, dtos.size(), acknowledged, dtos);
  }

  private CorrespondenceReadReceiptDto toOwnDto(CorrespondenceReadReceiptEntity row) {
    return new CorrespondenceReadReceiptDto(
        row.getId(),
        null,
        null,
        null,
        null,
        row.getFirstOpenedAt(),
        row.getLastOpenedAt(),
        row.getOpenCount(),
        row.getAcknowledgedAt(),
        row.getAcknowledgementComment());
  }

  private CorrespondenceReadReceiptDto toFullDto(CorrespondenceReadReceiptEntity row) {
    AppUserEntity u = row.getUser();
    return new CorrespondenceReadReceiptDto(
        row.getId(),
        u != null ? u.getId() : null,
        u != null ? u.getUsername() : null,
        u != null ? u.getFullNameAr() : null,
        u != null ? u.getFullNameEn() : null,
        row.getFirstOpenedAt(),
        row.getLastOpenedAt(),
        row.getOpenCount(),
        row.getAcknowledgedAt(),
        row.getAcknowledgementComment());
  }
}
