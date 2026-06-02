package com.gov.ac.feature.attachment.crypto;

import static com.gov.ac.feature.attachment.crypto.EnvironmentKeyProvider.GCM_TAG_BITS;

import com.gov.ac.feature.attachment.entity.AttachmentVersionEntity;
import com.gov.ac.feature.attachment.service.AttachmentContentStore;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Streams an {@link AttachmentVersionEntity}'s plaintext to an {@link OutputStream}, decrypting
 * AES-256-GCM ciphertext on the way. Versions written before V18 (no
 * {@code encryption_algo}) are streamed verbatim so legacy blobs keep working.
 *
 * <p>GCM authentication failures surface as {@link CryptoOperationException} so the caller can
 * convert to an audit row + HTTP 500.
 */
@Service
@RequiredArgsConstructor
public class AttachmentDecryptionService {

  private final AttachmentContentStore contentStore;
  private final KeyProvider keyProvider;

  /** True when the version row was created under the V18 encryption regime. */
  public static boolean isEncrypted(AttachmentVersionEntity version) {
    return version != null
        && version.getEncryptionAlgo() != null
        && !version.getEncryptionAlgo().isBlank();
  }

  public void streamPlaintext(AttachmentVersionEntity version, OutputStream out) throws IOException {
    if (version == null) {
      throw new IllegalArgumentException("version is required");
    }
    if (!isEncrypted(version)) {
      contentStore.copyToOutputStream(version.getStorageKey(), out);
      return;
    }
    if (!AttachmentEncryptionService.ALGORITHM_AES_256_GCM.equals(version.getEncryptionAlgo())) {
      throw new CryptoOperationException("Unsupported encryption algorithm: " + version.getEncryptionAlgo());
    }

    byte[] dek = keyProvider.unwrapDek(version.getEncryptionWrappedDek(), version.getEncryptionKeyRef());
    try {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(
          Cipher.DECRYPT_MODE,
          new SecretKeySpec(dek, "AES"),
          new GCMParameterSpec(GCM_TAG_BITS, version.getEncryptionIv()));

      ByteArrayOutputStream ciphertextBuffer = new ByteArrayOutputStream();
      contentStore.copyToOutputStream(version.getStorageKey(), ciphertextBuffer);
      byte[] plaintext = cipher.doFinal(ciphertextBuffer.toByteArray());
      out.write(plaintext);
      out.flush();
      java.util.Arrays.fill(plaintext, (byte) 0);
    } catch (GeneralSecurityException e) {
      throw new CryptoOperationException("Failed to decrypt attachment version " + version.getId(), e);
    } finally {
      java.util.Arrays.fill(dek, (byte) 0);
    }
  }
}
