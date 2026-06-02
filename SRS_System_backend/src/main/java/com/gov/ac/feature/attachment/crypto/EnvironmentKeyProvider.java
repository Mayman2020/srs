package com.gov.ac.feature.attachment.crypto;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Default {@link KeyProvider} that reads the 32-byte KEK from
 * {@code ac.attachment.encryption.kek-hex} (env var {@code AC_ATTACHMENT_KEK_HEX} in production).
 *
 * <p>When the property is empty the provider falls back to a deterministic dev key derived from
 * {@code "srs-dev-kek"} and logs a single WARN at startup. The fallback must never be used in
 * production — the runbook documents the env var as a hard requirement.
 */
@Component
@ConditionalOnMissingBean(value = KeyProvider.class, ignored = EnvironmentKeyProvider.class)
@Slf4j
public class EnvironmentKeyProvider implements KeyProvider {

  /** Length of the AES-256 KEK and DEK in bytes. */
  static final int AES_KEY_BYTES = 32;

  /** GCM nonce length in bytes (RFC 5116 §3.1). */
  static final int GCM_NONCE_BYTES = 12;

  /** GCM auth tag length in bits. */
  static final int GCM_TAG_BITS = 128;

  /** Fixed AAD for DEK wrapping; binds wrapped blob to its purpose. */
  private static final byte[] WRAP_AAD = "ac.attachment.dek".getBytes(StandardCharsets.UTF_8);

  private final AttachmentEncryptionProperties properties;
  private final SecretKeySpec kek;
  private final boolean fallbackKey;
  private final SecureRandom random = new SecureRandom();

  public EnvironmentKeyProvider(AttachmentEncryptionProperties properties) {
    this.properties = properties;
    byte[] keyBytes = resolveKekBytes(properties.kekHex());
    this.kek = new SecretKeySpec(keyBytes, "AES");
    this.fallbackKey = properties.kekHex().isEmpty();
  }

  @PostConstruct
  void announce() {
    if (fallbackKey) {
      log.warn(
          "[crypto] AC_ATTACHMENT_KEK_HEX is unset — using a deterministic DEV key for "
              + "attachment encryption. This is INSECURE; configure a 64-char hex value in production.");
    } else {
      log.info("[crypto] EnvironmentKeyProvider initialized with KEK ref {}", properties.kekRef());
    }
  }

  @Override
  public String currentKekRef() {
    return properties.kekRef();
  }

  @Override
  public WrappedDek wrapDek(byte[] rawDek) {
    if (rawDek == null || rawDek.length != AES_KEY_BYTES) {
      throw new CryptoOperationException("DEK must be exactly 32 bytes");
    }
    try {
      byte[] iv = new byte[GCM_NONCE_BYTES];
      random.nextBytes(iv);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, kek, new GCMParameterSpec(GCM_TAG_BITS, iv));
      cipher.updateAAD(WRAP_AAD);
      byte[] ciphertext = cipher.doFinal(rawDek);
      byte[] envelope = new byte[GCM_NONCE_BYTES + ciphertext.length];
      System.arraycopy(iv, 0, envelope, 0, GCM_NONCE_BYTES);
      System.arraycopy(ciphertext, 0, envelope, GCM_NONCE_BYTES, ciphertext.length);
      return new WrappedDek(envelope, properties.kekRef());
    } catch (GeneralSecurityException e) {
      throw new CryptoOperationException("Failed to wrap DEK", e);
    }
  }

  @Override
  public byte[] unwrapDek(byte[] wrappedDek, String keyRef) {
    if (wrappedDek == null || wrappedDek.length <= GCM_NONCE_BYTES) {
      throw new CryptoOperationException("Wrapped DEK envelope is malformed");
    }
    if (keyRef == null || !keyRef.equals(properties.kekRef())) {
      throw new CryptoOperationException("Unknown KEK reference: " + keyRef);
    }
    try {
      byte[] iv = new byte[GCM_NONCE_BYTES];
      System.arraycopy(wrappedDek, 0, iv, 0, GCM_NONCE_BYTES);
      byte[] ciphertext = new byte[wrappedDek.length - GCM_NONCE_BYTES];
      System.arraycopy(wrappedDek, GCM_NONCE_BYTES, ciphertext, 0, ciphertext.length);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, kek, new GCMParameterSpec(GCM_TAG_BITS, iv));
      cipher.updateAAD(WRAP_AAD);
      return cipher.doFinal(ciphertext);
    } catch (GeneralSecurityException e) {
      throw new CryptoOperationException("Failed to unwrap DEK", e);
    }
  }

  private static byte[] resolveKekBytes(String hex) {
    if (hex == null || hex.isEmpty()) {
      try {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest("srs-dev-kek".getBytes(StandardCharsets.UTF_8));
      } catch (GeneralSecurityException e) {
        throw new CryptoOperationException("SHA-256 unavailable", e);
      }
    }
    if (hex.length() != AES_KEY_BYTES * 2) {
      throw new CryptoOperationException(
          "ac.attachment.encryption.kek-hex must be exactly " + (AES_KEY_BYTES * 2) + " hex chars");
    }
    byte[] out = new byte[AES_KEY_BYTES];
    for (int i = 0; i < AES_KEY_BYTES; i++) {
      int hi = Character.digit(hex.charAt(i * 2), 16);
      int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
      if (hi < 0 || lo < 0) {
        throw new CryptoOperationException("ac.attachment.encryption.kek-hex contains non-hex chars");
      }
      out[i] = (byte) ((hi << 4) | lo);
    }
    return out;
  }

  /** Nested config so the SPI bean self-registers without modifying the boot class. */
  @Configuration
  static class Registration {
    // Intentionally empty: presence makes ApplicationContext component-scan pick up the @Component.
  }
}
