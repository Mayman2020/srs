package com.gov.ac.feature.attachment.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Encryption settings bound from {@code ac.attachment.encryption.*}. */
@ConfigurationProperties(prefix = "ac.attachment.encryption")
public record AttachmentEncryptionProperties(
    /** Master switch. When false, uploads are stored as plaintext (legacy compatibility mode). */
    Boolean enabled,
    /** KEK identifier persisted on every encrypted row. */
    String kekRef,
    /**
     * Hex-encoded 32-byte master KEK. Set via {@code AC_ATTACHMENT_KEK_HEX} env var in prod. An
     * empty value falls back to a deterministic dev key (and the provider logs WARN once).
     */
    String kekHex) {

  public AttachmentEncryptionProperties {
    enabled = enabled == null ? Boolean.TRUE : enabled;
    kekRef = (kekRef == null || kekRef.isBlank()) ? "KEK_V1" : kekRef;
    kekHex = kekHex == null ? "" : kekHex.trim();
  }
}
