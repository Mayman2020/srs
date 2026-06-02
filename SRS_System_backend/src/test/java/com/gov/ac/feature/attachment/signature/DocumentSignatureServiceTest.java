package com.gov.ac.feature.attachment.signature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.feature.attachment.crypto.AttachmentDecryptionService;
import com.gov.ac.feature.attachment.entity.AttachmentEntity;
import com.gov.ac.feature.attachment.entity.AttachmentVersionEntity;
import com.gov.ac.feature.attachment.repository.AttachmentRepository;
import com.gov.ac.feature.attachment.repository.AttachmentVersionRepository;
import com.gov.ac.feature.attachment.service.AttachmentContentStore;
import com.gov.ac.feature.attachment.signature.dto.DocumentSignatureDto;
import com.gov.ac.feature.attachment.signature.entity.DocumentSignatureEntity;
import com.gov.ac.feature.attachment.signature.repository.DocumentSignatureRepository;
import com.gov.ac.feature.audit.service.AuditTrailService;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.correspondence.security.CorrespondenceViewAuthorization;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentSignatureServiceTest {

  @Mock private DocumentSignatureRepository signatureRepository;
  @Mock private AttachmentRepository attachmentRepository;
  @Mock private AttachmentVersionRepository attachmentVersionRepository;
  @Mock private AppUserRepository appUserRepository;
  @Mock private CorrespondenceViewAuthorization correspondenceViewAuthorization;
  @Mock private AttachmentDecryptionService decryptionService;
  @Mock private AttachmentContentStore contentStore;
  @Mock private AuditTrailService auditTrailService;

  private SigningKeyProvider signingKeyProvider;
  private DocumentSignatureService service;

  private final byte[] plaintext = "signed payload".getBytes();
  private String plaintextHashHex;

  private UUID userId;
  private AttachmentEntity attachment;
  private AttachmentVersionEntity version;
  private CorrespondenceEntity correspondence;

  @BeforeEach
  void setUp() throws Exception {
    signingKeyProvider =
        new EnvironmentSigningKeyProvider(new SignatureProperties("ED25519", "SIGN_V1", "", ""));
    service =
        new DocumentSignatureService(
            signatureRepository,
            attachmentRepository,
            attachmentVersionRepository,
            appUserRepository,
            correspondenceViewAuthorization,
            decryptionService,
            contentStore,
            signingKeyProvider,
            auditTrailService);

    userId = UUID.randomUUID();
    AppUserEntity user = new AppUserEntity();
    user.setId(userId);
    user.setActive(true);

    correspondence = new CorrespondenceEntity();
    correspondence.setId(UUID.randomUUID());

    attachment = new AttachmentEntity();
    attachment.setId(101L);
    attachment.setCorrespondence(correspondence);
    attachment.setCurrentVersionId(202L);

    plaintextHashHex = toHex(MessageDigest.getInstance("SHA-256").digest(plaintext));

    version = new AttachmentVersionEntity();
    version.setId(202L);
    version.setAttachment(attachment);
    version.setStorageKey("file");
    version.setByteSize((long) plaintext.length);
    version.setPlaintextSha256(plaintextHashHex);

    when(appUserRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
    when(attachmentRepository.findByIdAndDeletedAtIsNullWithCorrespondenceForAuth(101L))
        .thenReturn(Optional.of(attachment));
    when(attachmentVersionRepository.findByIdAndDeletedAtIsNullWithAttachment(202L))
        .thenReturn(Optional.of(version));
    when(signatureRepository.findActiveByVersionAndSigner(202L, userId)).thenReturn(Optional.empty());
    when(signatureRepository.save(any(DocumentSignatureEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    doAnswer(
            inv -> {
              OutputStream out = inv.getArgument(1);
              out.write(plaintext);
              return null;
            })
        .when(contentStore)
        .copyToOutputStream(any(), any(OutputStream.class));
  }

  @Test
  void createSignsAndVerifies() {
    DocumentSignatureDto dto = service.create(userId, 101L);

    assertThat(dto.algorithm()).isEqualTo("ED25519");
    assertThat(dto.canonicalHashSha256()).isEqualTo(plaintextHashHex);
    assertThat(dto.verificationStatus()).isEqualTo("VERIFIED");
    assertThat(dto.status()).isEqualTo("VALID");
  }

  @Test
  void verifyFlipsToFailedWhenPlaintextDrifts() throws Exception {
    DocumentSignatureDto created = service.create(userId, 101L);

    // Simulate tampering: content store now returns different bytes.
    byte[] tampered = "tampered".getBytes();
    doAnswer(
            inv -> {
              OutputStream out = inv.getArgument(1);
              out.write(tampered);
              return null;
            })
        .when(contentStore)
        .copyToOutputStream(any(), any(OutputStream.class));

    DocumentSignatureEntity persisted = capturePersisted(created.id());
    when(signatureRepository.findByIdLoaded(created.id())).thenReturn(Optional.of(persisted));

    DocumentSignatureDto verified = service.verify(userId, created.id());
    assertThat(verified.verificationStatus()).isEqualTo("FAILED");
    assertThat(verified.verificationDetail()).contains("hash drift");
  }

  @Test
  void revokeFlipsStatus() {
    DocumentSignatureDto created = service.create(userId, 101L);
    DocumentSignatureEntity persisted = capturePersisted(created.id());
    when(signatureRepository.findByIdLoaded(created.id())).thenReturn(Optional.of(persisted));

    DocumentSignatureDto revoked = service.revoke(userId, created.id());
    assertThat(revoked.status()).isEqualTo("REVOKED");
  }

  @Test
  void duplicateSignBySameUserIsRejected() {
    DocumentSignatureEntity existing = new DocumentSignatureEntity();
    existing.setStatus("VALID");
    when(signatureRepository.findActiveByVersionAndSigner(202L, userId))
        .thenReturn(Optional.of(existing));
    assertThatThrownBy(() -> service.create(userId, 101L))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("already signed");
  }

  /**
   * Re-runs the create path on a fresh signature entity and returns it so verify/revoke tests have
   * a row to operate on; we cannot capture the original because the service mutates the captured
   * instance in-place when calling save.
   */
  private DocumentSignatureEntity capturePersisted(UUID id) {
    DocumentSignatureEntity row = new DocumentSignatureEntity();
    row.setId(id);
    row.setAttachmentVersion(version);
    AppUserEntity signer = new AppUserEntity();
    signer.setId(userId);
    row.setSigner(signer);
    row.setAlgorithm("ED25519");
    row.setCanonicalHashSha256(plaintextHashHex);
    SigningKeyProvider.SignatureResult sig =
        signingKeyProvider.sign(hexToBytes(plaintextHashHex));
    row.setSignatureBytes(sig.signatureBytes());
    row.setKeyRef("SIGN_V1");
    row.setSignedAt(java.time.Instant.now());
    row.setStatus("VALID");
    row.setVerificationStatus("VERIFIED");
    return row;
  }

  private static String toHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      sb.append(Character.forDigit((b >> 4) & 0xF, 16));
      sb.append(Character.forDigit(b & 0xF, 16));
    }
    return sb.toString();
  }

  private static byte[] hexToBytes(String hex) {
    int len = hex.length();
    byte[] out = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      out[i / 2] =
          (byte) ((Character.digit(hex.charAt(i), 16) << 4) | Character.digit(hex.charAt(i + 1), 16));
    }
    return out;
  }
}
