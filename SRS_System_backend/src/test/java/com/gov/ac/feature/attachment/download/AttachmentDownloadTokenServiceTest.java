package com.gov.ac.feature.attachment.download;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gov.ac.common.api.ForbiddenException;
import com.gov.ac.feature.attachment.access.entity.AttachmentAccessLogEntity;
import com.gov.ac.feature.attachment.access.service.AttachmentAccessLogService;
import com.gov.ac.feature.attachment.download.entity.AttachmentDownloadTokenEntity;
import com.gov.ac.feature.attachment.download.repository.AttachmentDownloadTokenRepository;
import com.gov.ac.feature.attachment.entity.AttachmentEntity;
import com.gov.ac.feature.attachment.entity.AttachmentVersionEntity;
import com.gov.ac.feature.attachment.repository.AttachmentRepository;
import com.gov.ac.feature.attachment.repository.AttachmentVersionRepository;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.correspondence.security.CorrespondenceViewAuthorization;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AttachmentDownloadTokenServiceTest {

  @Mock private AttachmentDownloadTokenRepository tokenRepository;
  @Mock private AttachmentRepository attachmentRepository;
  @Mock private AttachmentVersionRepository attachmentVersionRepository;
  @Mock private AppUserRepository appUserRepository;
  @Mock private CorrespondenceViewAuthorization correspondenceViewAuthorization;
  @Mock private AttachmentAccessLogService accessLogService;
  @Mock private HttpServletRequest request;

  private AttachmentDownloadTokenService service;

  private UUID userId;
  private AttachmentEntity attachment;
  private AttachmentVersionEntity version;
  private CorrespondenceEntity correspondence;

  @BeforeEach
  void setUp() {
    AttachmentDownloadTokenProperties props =
        new AttachmentDownloadTokenProperties(60L, 600_000L);
    service =
        new AttachmentDownloadTokenService(
            tokenRepository,
            attachmentRepository,
            attachmentVersionRepository,
            appUserRepository,
            correspondenceViewAuthorization,
            accessLogService,
            props);

    userId = UUID.randomUUID();
    correspondence = new CorrespondenceEntity();
    correspondence.setId(UUID.randomUUID());
    attachment = new AttachmentEntity();
    attachment.setId(101L);
    attachment.setCorrespondence(correspondence);
    attachment.setCurrentVersionId(202L);
    version = new AttachmentVersionEntity();
    version.setId(202L);
    version.setAttachment(attachment);
  }

  @Test
  void issueAndConsumeHappyPath() {
    AppUserEntity user = activeUser(userId);
    when(appUserRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
    when(attachmentRepository.findByIdAndDeletedAtIsNullWithCorrespondenceForAuth(101L))
        .thenReturn(Optional.of(attachment));
    when(attachmentVersionRepository.findByIdAndDeletedAtIsNullWithAttachment(202L))
        .thenReturn(Optional.of(version));
    when(tokenRepository.save(any(AttachmentDownloadTokenEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    var intent = service.issue(101L, userId, request);
    assertThat(intent.token()).isNotBlank();
    assertThat(intent.expiresAt()).isAfter(Instant.now());

    ArgumentCaptor<AttachmentDownloadTokenEntity> saved =
        ArgumentCaptor.forClass(AttachmentDownloadTokenEntity.class);
    verify(tokenRepository).save(saved.capture());
    AttachmentDownloadTokenEntity row = saved.getValue();
    assertThat(row.getTokenHash()).hasSize(64);
    assertThat(row.getUserId()).isEqualTo(userId);

    // For consume(), make the token findable by hash and re-attach the version.
    when(tokenRepository.findByTokenHash(row.getTokenHash())).thenReturn(Optional.of(row));

    AttachmentVersionEntity consumed = service.consume(intent.token(), userId, request);
    assertThat(consumed.getId()).isEqualTo(version.getId());
    assertThat(row.getConsumedAt()).isNotNull();
  }

  @Test
  void replayAfterConsumeIsRejected() {
    AttachmentDownloadTokenEntity row = readyTokenRow();
    row.setConsumedAt(Instant.now().minusSeconds(1));
    when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(row));

    assertThatThrownBy(() -> service.consume("opaque", userId, request))
        .isInstanceOf(ForbiddenException.class);
    verify(accessLogService)
        .record(eq(version), eq(userId), eq(AttachmentAccessLogEntity.ACTION_DOWNLOAD), eq(false), any());
  }

  @Test
  void expiredTokenIsRejected() {
    AttachmentDownloadTokenEntity row = readyTokenRow();
    row.setExpiresAt(Instant.now().minusSeconds(1));
    when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(row));

    assertThatThrownBy(() -> service.consume("opaque", userId, request))
        .isInstanceOf(ForbiddenException.class);
    verify(accessLogService)
        .record(eq(version), eq(userId), eq(AttachmentAccessLogEntity.ACTION_DOWNLOAD), eq(false), any());
  }

  @Test
  void userMismatchIsRejected() {
    AttachmentDownloadTokenEntity row = readyTokenRow();
    row.setUserId(UUID.randomUUID());
    when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(row));

    assertThatThrownBy(() -> service.consume("opaque", userId, request))
        .isInstanceOf(ForbiddenException.class);
    verify(accessLogService, atLeastOnce())
        .record(any(), eq(userId), eq(AttachmentAccessLogEntity.ACTION_DOWNLOAD), eq(false), any());
  }

  @Test
  void revokedTokenIsRejected() {
    AttachmentDownloadTokenEntity row = readyTokenRow();
    row.setRevokedAt(Instant.now());
    when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(row));

    assertThatThrownBy(() -> service.consume("opaque", userId, request))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void unknownTokenDoesNotLogToAccessLog() {
    when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.consume("ghost", userId, request));
    verify(accessLogService, never()).record(any(), any(), anyString(), anyBoolean(), any());
  }

  private AttachmentDownloadTokenEntity readyTokenRow() {
    AttachmentDownloadTokenEntity row = new AttachmentDownloadTokenEntity();
    row.setId(UUID.randomUUID());
    row.setTokenHash("hash");
    row.setAttachment(attachment);
    row.setAttachmentVersion(version);
    row.setUserId(userId);
    row.setIssuedAt(Instant.now().minusSeconds(10));
    row.setExpiresAt(Instant.now().plusSeconds(60));
    return row;
  }

  private static AppUserEntity activeUser(UUID id) {
    AppUserEntity u = new AppUserEntity();
    u.setId(id);
    u.setActive(true);
    return u;
  }
}
