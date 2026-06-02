package com.gov.ac.feature.attachment.crypto;

import static com.gov.ac.feature.attachment.crypto.EnvironmentKeyProvider.AES_KEY_BYTES;
import static com.gov.ac.feature.attachment.crypto.EnvironmentKeyProvider.GCM_NONCE_BYTES;
import static com.gov.ac.feature.attachment.crypto.EnvironmentKeyProvider.GCM_TAG_BITS;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Encrypts an upload to AES-256-GCM and atomically lands the ciphertext on disk. The returned
 * {@link EncryptedBlobMetadata} carries everything the persistence layer needs to round-trip the
 * blob: wrapped DEK, IV, plaintext and ciphertext digests, byte sizes.
 *
 * <p>Auth tag is appended inline to the ciphertext by the JCE provider so the on-disk envelope is
 * {@code ciphertext || tag} — no header is required.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentEncryptionService {

  /** Canonical algorithm code persisted on {@code attachment_version.encryption_algo}. */
  public static final String ALGORITHM_AES_256_GCM = "AES_256_GCM";

  private final KeyProvider keyProvider;
  private final SecureRandom random = new SecureRandom();

  /**
   * Streams {@code plaintext} through AES-256-GCM into {@code target}, computing both digests on
   * the fly. The plaintext stream is closed by the caller.
   */
  public EncryptedBlobMetadata encrypt(InputStream plaintext, Path target) throws IOException {
    if (plaintext == null) {
      throw new IllegalArgumentException("plaintext stream is required");
    }
    if (target == null) {
      throw new IllegalArgumentException("target path is required");
    }

    byte[] dek = new byte[AES_KEY_BYTES];
    random.nextBytes(dek);
    byte[] iv = new byte[GCM_NONCE_BYTES];
    random.nextBytes(iv);

    KeyProvider.WrappedDek wrapped = keyProvider.wrapDek(dek);

    MessageDigest plainDigest;
    MessageDigest cipherDigest;
    try {
      plainDigest = MessageDigest.getInstance("SHA-256");
      cipherDigest = MessageDigest.getInstance("SHA-256");
    } catch (GeneralSecurityException e) {
      zero(dek);
      throw new CryptoOperationException("SHA-256 unavailable", e);
    }

    Path tmp = Files.createTempFile(target.getParent(), ".enc-", ".part");
    long plainSize = 0L;
    long cipherSize;
    try (OutputStream fileOut = new BufferedOutputStream(Files.newOutputStream(tmp));
        DigestingOutputStream digestOut = new DigestingOutputStream(fileOut, cipherDigest)) {

      Cipher cipher;
      try {
        cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(dek, "AES"),
            new GCMParameterSpec(GCM_TAG_BITS, iv));
      } catch (GeneralSecurityException e) {
        throw new CryptoOperationException("Failed to initialize AES-256-GCM encryption", e);
      }

      try (CipherOutputStream cipherOut = new CipherOutputStream(digestOut, cipher)) {
        byte[] buf = new byte[16 * 1024];
        int n;
        while ((n = plaintext.read(buf)) > 0) {
          plainDigest.update(buf, 0, n);
          plainSize += n;
          cipherOut.write(buf, 0, n);
        }
      }
      cipherSize = digestOut.bytesWritten();
    } catch (IOException e) {
      Files.deleteIfExists(tmp);
      throw e;
    } catch (RuntimeException e) {
      Files.deleteIfExists(tmp);
      throw e;
    } finally {
      zero(dek);
    }

    try {
      Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (java.nio.file.AtomicMoveNotSupportedException e) {
      Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
    }

    return new EncryptedBlobMetadata(
        ALGORITHM_AES_256_GCM,
        wrapped.keyRef(),
        wrapped.wrapped(),
        iv,
        toHex(plainDigest.digest()),
        toHex(cipherDigest.digest()),
        plainSize,
        cipherSize);
  }

  private static void zero(byte[] bytes) {
    if (bytes != null) {
      java.util.Arrays.fill(bytes, (byte) 0);
    }
  }

  private static String toHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      sb.append(Character.forDigit((b >> 4) & 0xF, 16));
      sb.append(Character.forDigit(b & 0xF, 16));
    }
    return sb.toString();
  }

  /** Captures SHA-256 of the bytes flowing through to disk. */
  private static final class DigestingOutputStream extends OutputStream {
    private final OutputStream delegate;
    private final MessageDigest digest;
    private long bytesWritten;

    DigestingOutputStream(OutputStream delegate, MessageDigest digest) {
      this.delegate = delegate;
      this.digest = digest;
    }

    long bytesWritten() {
      return bytesWritten;
    }

    @Override
    public void write(int b) throws IOException {
      delegate.write(b);
      digest.update((byte) b);
      bytesWritten++;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      delegate.write(b, off, len);
      digest.update(b, off, len);
      bytesWritten += len;
    }

    @Override
    public void flush() throws IOException {
      delegate.flush();
    }

    @Override
    public void close() throws IOException {
      delegate.close();
    }
  }
}
