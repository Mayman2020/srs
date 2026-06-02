package com.gov.ac.feature.attachment.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gov.ac.feature.attachment.entity.AttachmentVersionEntity;
import com.gov.ac.feature.attachment.service.AttachmentContentStore;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AttachmentEncryptionServiceTest {

  private final KeyProvider keyProvider =
      new EnvironmentKeyProvider(new AttachmentEncryptionProperties(true, "KEK_V1", ""));
  private final AttachmentEncryptionService encryption = new AttachmentEncryptionService(keyProvider);

  @Test
  void roundTrip(@TempDir Path tmp) throws Exception {
    byte[] plaintext = "hello, classified world".getBytes();
    Path target = tmp.resolve("blob.enc");
    EncryptedBlobMetadata metadata = encryption.encrypt(new ByteArrayInputStream(plaintext), target);

    assertThat(metadata.algorithm()).isEqualTo("AES_256_GCM");
    assertThat(metadata.keyRef()).isEqualTo("KEK_V1");
    assertThat(metadata.iv()).hasSize(12);
    assertThat(metadata.plaintextSha256()).hasSize(64);
    assertThat(metadata.plaintextByteSize()).isEqualTo(plaintext.length);
    assertThat(Files.size(target)).isEqualTo(metadata.ciphertextByteSize());

    AttachmentVersionEntity version = buildVersion(metadata);
    AttachmentDecryptionService decryption =
        new AttachmentDecryptionService(staticStore(target), keyProvider);

    ByteArrayOutputStream sink = new ByteArrayOutputStream();
    decryption.streamPlaintext(version, sink);
    assertThat(sink.toByteArray()).containsExactly(plaintext);
  }

  @Test
  void freshIvPerCall(@TempDir Path tmp) throws Exception {
    byte[] data = new byte[1024];
    EncryptedBlobMetadata a = encryption.encrypt(new ByteArrayInputStream(data), tmp.resolve("a"));
    EncryptedBlobMetadata b = encryption.encrypt(new ByteArrayInputStream(data), tmp.resolve("b"));
    assertThat(a.iv()).isNotEqualTo(b.iv());
    assertThat(a.wrappedDek()).isNotEqualTo(b.wrappedDek());
  }

  @Test
  void tamperingFailsGcm(@TempDir Path tmp) throws Exception {
    byte[] plaintext = "needs integrity".getBytes();
    Path target = tmp.resolve("blob.enc");
    EncryptedBlobMetadata metadata = encryption.encrypt(new ByteArrayInputStream(plaintext), target);

    byte[] bytes = Files.readAllBytes(target);
    bytes[bytes.length - 1] ^= 0x01;
    Files.write(target, bytes);

    AttachmentVersionEntity version = buildVersion(metadata);
    AttachmentDecryptionService decryption =
        new AttachmentDecryptionService(staticStore(target), keyProvider);

    assertThatThrownBy(() -> decryption.streamPlaintext(version, new ByteArrayOutputStream()))
        .isInstanceOf(CryptoOperationException.class);
  }

  @Test
  void plaintextHashMatchesIndependentDigest(@TempDir Path tmp) throws Exception {
    byte[] plaintext = "0123456789abcdef".repeat(10).getBytes();
    EncryptedBlobMetadata metadata =
        encryption.encrypt(new ByteArrayInputStream(plaintext), tmp.resolve("a"));
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] expected = md.digest(plaintext);
    StringBuilder hex = new StringBuilder();
    for (byte b : expected) {
      hex.append(Character.forDigit((b >> 4) & 0xF, 16));
      hex.append(Character.forDigit(b & 0xF, 16));
    }
    assertThat(metadata.plaintextSha256()).isEqualTo(hex.toString());
  }

  private static AttachmentVersionEntity buildVersion(EncryptedBlobMetadata metadata) {
    AttachmentVersionEntity v = new AttachmentVersionEntity();
    v.setId(1L);
    v.setStorageKey("ignored");
    v.setByteSize(metadata.plaintextByteSize());
    v.setEncryptionAlgo(metadata.algorithm());
    v.setEncryptionKeyRef(metadata.keyRef());
    v.setEncryptionWrappedDek(metadata.wrappedDek());
    v.setEncryptionIv(metadata.iv());
    v.setPlaintextSha256(metadata.plaintextSha256());
    v.setCiphertextSha256(metadata.ciphertextSha256());
    return v;
  }

  private static AttachmentContentStore staticStore(Path path) {
    return new AttachmentContentStore() {
      @Override
      public void copyToOutputStream(String storageKey, OutputStream out) throws IOException {
        Files.copy(path, out);
      }
    };
  }
}
