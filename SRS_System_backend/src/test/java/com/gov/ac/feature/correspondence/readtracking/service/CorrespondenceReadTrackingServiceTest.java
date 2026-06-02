package com.gov.ac.feature.correspondence.readtracking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gov.ac.feature.audit.dto.CreateAuditEventRequestDto;
import com.gov.ac.feature.audit.service.AuditTrailService;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.correspondence.readtracking.dto.CorrespondenceReadReceiptDto;
import com.gov.ac.feature.correspondence.readtracking.entity.CorrespondenceReadReceiptEntity;
import com.gov.ac.feature.correspondence.readtracking.repository.CorrespondenceReadReceiptRepository;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import com.gov.ac.feature.correspondence.security.CorrespondenceViewAuthorization;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CorrespondenceReadTrackingServiceTest {

  @Mock private CorrespondenceReadReceiptRepository readReceiptRepository;
  @Mock private CorrespondenceRepository correspondenceRepository;
  @Mock private AppUserRepository appUserRepository;
  @Mock private CorrespondenceViewAuthorization correspondenceViewAuthorization;
  @Mock private AuditTrailService auditTrailService;

  @InjectMocks private CorrespondenceReadTrackingService service;

  private UUID correspondenceId;
  private UUID userId;
  private CorrespondenceEntity correspondence;
  private AppUserEntity user;

  @BeforeEach
  void setUp() {
    correspondenceId = UUID.randomUUID();
    userId = UUID.randomUUID();
    correspondence = new CorrespondenceEntity();
    correspondence.setId(correspondenceId);
    user = new AppUserEntity();
    user.setId(userId);
    user.setActive(true);
  }

  @Test
  void firstOpenCreatesReceiptAndEmitsAuditEvent() {
    when(readReceiptRepository.findActiveByCorrespondenceAndUser(correspondenceId, userId))
        .thenReturn(Optional.empty());
    when(correspondenceRepository.findById(correspondenceId)).thenReturn(Optional.of(correspondence));
    when(appUserRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
    when(readReceiptRepository.save(any(CorrespondenceReadReceiptEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    service.recordOpen(correspondenceId, userId);

    verify(readReceiptRepository)
        .save(
            argThat(
                row ->
                    row.getOpenCount() == 1
                        && row.getFirstOpenedAt() != null
                        && row.getLastOpenedAt() != null));
    verify(auditTrailService)
        .append(
            argThat(
                (CreateAuditEventRequestDto evt) ->
                    "CORRESPONDENCE_FIRST_OPENED".equals(evt.actionCode())
                        && correspondenceId.toString().equals(evt.resourceId())
                        && userId.toString().equals(evt.actorUserId())));
  }

  @Test
  void subsequentOpenIncrementsCountWithoutEmittingAudit() {
    CorrespondenceReadReceiptEntity existing = new CorrespondenceReadReceiptEntity();
    existing.setCorrespondence(correspondence);
    existing.setUser(user);
    Instant prev = Instant.now().minusSeconds(60);
    existing.setFirstOpenedAt(prev);
    existing.setLastOpenedAt(prev);
    existing.setOpenCount(1);
    when(readReceiptRepository.findActiveByCorrespondenceAndUser(correspondenceId, userId))
        .thenReturn(Optional.of(existing));
    when(readReceiptRepository.save(any(CorrespondenceReadReceiptEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    service.recordOpen(correspondenceId, userId);

    verify(readReceiptRepository)
        .save(
            argThat(
                row ->
                    row.getOpenCount() == 2
                        && row.getFirstOpenedAt().equals(prev)
                        && row.getLastOpenedAt().isAfter(prev)));
    verify(auditTrailService, never()).append(any());
  }

  @Test
  void acknowledgeSetsAcknowledgedAtOnceAndEmitsSingleAuditEvent() {
    when(correspondenceRepository.findById(correspondenceId)).thenReturn(Optional.of(correspondence));
    when(appUserRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
    when(readReceiptRepository.findActiveByCorrespondenceAndUser(correspondenceId, userId))
        .thenReturn(Optional.empty());
    when(readReceiptRepository.save(any(CorrespondenceReadReceiptEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    CorrespondenceReadReceiptDto first = service.acknowledge(correspondenceId, userId, "noted");
    assertThat(first.acknowledgedAt()).isNotNull();
    assertThat(first.acknowledgementComment()).isEqualTo("noted");

    CorrespondenceReadReceiptEntity ackedRow = new CorrespondenceReadReceiptEntity();
    ackedRow.setCorrespondence(correspondence);
    ackedRow.setUser(user);
    Instant ackTime = first.acknowledgedAt();
    ackedRow.setFirstOpenedAt(ackTime);
    ackedRow.setLastOpenedAt(ackTime);
    ackedRow.setOpenCount(1);
    ackedRow.setAcknowledgedAt(ackTime);
    ackedRow.setAcknowledgementComment("noted");
    when(readReceiptRepository.findActiveByCorrespondenceAndUser(correspondenceId, userId))
        .thenReturn(Optional.of(ackedRow));

    CorrespondenceReadReceiptDto second = service.acknowledge(correspondenceId, userId, "noted again");
    assertThat(second.acknowledgedAt()).isEqualTo(ackTime);

    verify(auditTrailService, times(1))
        .append(
            argThat(
                (CreateAuditEventRequestDto evt) -> "CORRESPONDENCE_ACKED".equals(evt.actionCode())));
  }
}
