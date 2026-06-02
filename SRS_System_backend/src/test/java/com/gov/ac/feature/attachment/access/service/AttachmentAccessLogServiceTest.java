package com.gov.ac.feature.attachment.access.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gov.ac.feature.attachment.access.entity.AttachmentAccessLogEntity;
import com.gov.ac.feature.attachment.access.repository.AttachmentAccessLogRepository;
import com.gov.ac.feature.attachment.entity.AttachmentEntity;
import com.gov.ac.feature.attachment.entity.AttachmentVersionEntity;
import com.gov.ac.feature.audit.dto.CreateAuditEventRequestDto;
import com.gov.ac.feature.audit.service.AuditTrailService;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AttachmentAccessLogServiceTest {

  @Mock private AttachmentAccessLogRepository accessLogRepository;
  @Mock private AppUserRepository appUserRepository;
  @Mock private AuditTrailService auditTrailService;
  @Mock private HttpServletRequest request;

  @InjectMocks private AttachmentAccessLogService service;

  private UUID userId;
  private UUID correspondenceId;
  private AttachmentVersionEntity version;
  private AttachmentEntity attachment;
  private CorrespondenceEntity correspondence;
  private AppUserEntity user;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    correspondenceId = UUID.randomUUID();

    correspondence = new CorrespondenceEntity();
    correspondence.setId(correspondenceId);

    attachment = new AttachmentEntity();
    attachment.setId(101L);
    attachment.setCorrespondence(correspondence);

    version = new AttachmentVersionEntity();
    version.setId(202L);
    version.setAttachment(attachment);

    user = new AppUserEntity();
    user.setId(userId);
  }

  @Test
  void recordPersistsRowWithExtractedIpAndUserAgentAndEmitsAudit() {
    when(appUserRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
    when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 10.0.0.2");
    when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
    when(accessLogRepository.save(any(AttachmentAccessLogEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    service.record(version, userId, AttachmentAccessLogEntity.ACTION_DOWNLOAD, true, request);

    verify(accessLogRepository)
        .save(
            argThat(
                row ->
                    "DOWNLOAD".equals(row.getActionCode())
                        && row.isSuccess()
                        && "10.0.0.1".equals(row.getIpAddress())
                        && "Mozilla/5.0".equals(row.getUserAgent())
                        && row.getOccurredAt() != null));
    verify(auditTrailService)
        .append(
            argThat(
                (CreateAuditEventRequestDto evt) ->
                    "ATTACHMENT_DOWNLOADED".equals(evt.actionCode())
                        && "ATTACHMENT".equals(evt.resourceType())
                        && "202".equals(evt.resourceId())));
  }

  @Test
  void recordWithNullRequestIsSafe() {
    when(appUserRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
    when(accessLogRepository.save(any(AttachmentAccessLogEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    service.record(version, userId, AttachmentAccessLogEntity.ACTION_DOWNLOAD, true, null);

    verify(accessLogRepository)
        .save(
            argThat(
                row ->
                    row.getIpAddress() == null
                        && row.getUserAgent() == null
                        && row.isSuccess()));
  }

  @Test
  void unknownUserIsSilentlyIgnored() {
    when(appUserRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

    service.record(version, userId, AttachmentAccessLogEntity.ACTION_DOWNLOAD, true, request);

    verify(accessLogRepository, never()).save(any());
    verify(auditTrailService, never()).append(any());
  }

  @Test
  void clientIpExtractionPrefersForwardedHeader() {
    when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1");
    assertThat(AttachmentAccessLogService.extractClientIp(request)).isEqualTo("203.0.113.1");
  }

  @Test
  void clientIpExtractionFallsBackToRemoteAddr() {
    when(request.getHeader("X-Forwarded-For")).thenReturn(null);
    when(request.getRemoteAddr()).thenReturn("198.51.100.7");
    assertThat(AttachmentAccessLogService.extractClientIp(request)).isEqualTo("198.51.100.7");
  }

  @Test
  void clientIpExtractionWithNullRequestReturnsNull() {
    assertThat(AttachmentAccessLogService.extractClientIp(null)).isNull();
  }
}
