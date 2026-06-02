package com.gov.ac.feature.attachment.signature;

/**
 * SPI for digital-signature operations. The default bean ({@link EnvironmentSigningKeyProvider})
 * holds an ED25519 keypair loaded from configuration; HSM / PKI integrations replace the bean
 * without touching the calling services.
 */
public interface SigningKeyProvider {

  /** Canonical algorithm code persisted on every signature row. */
  String algorithm();

  /** Stable identifier of the current signing key. Persisted on every signature row. */
  String currentKeyRef();

  /** PEM of the public key portion (optional). May be empty for HSM-backed providers. */
  String publicKeyPem();

  /** Signs {@code hash} (raw bytes, no further hashing). */
  SignatureResult sign(byte[] hash);

  /**
   * Verifies a previously generated signature. {@code keyRef} identifies which historical key to
   * use; implementations that rotate must keep old public keys available.
   */
  boolean verify(byte[] hash, byte[] signatureBytes, String keyRef, String algorithm);

  record SignatureResult(String algorithm, byte[] signatureBytes, String keyRef, String certificatePem) {}
}
