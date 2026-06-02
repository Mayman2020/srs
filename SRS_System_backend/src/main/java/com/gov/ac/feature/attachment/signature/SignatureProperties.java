package com.gov.ac.feature.attachment.signature;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings bound from {@code ac.signature.*}. */
@ConfigurationProperties(prefix = "ac.signature")
public record SignatureProperties(
    /** Canonical algorithm identifier persisted on each row (e.g. {@code ED25519}). */
    String algorithm,
    /** Identifier of the signing key used to produce signatures. */
    String keyRef,
    /** PKCS#8 PEM of the private key. Empty value triggers the dev keypair fallback + WARN. */
    String privateKeyPem,
    /** X.509 PEM of the public key (paired with {@link #privateKeyPem()}). */
    String publicKeyPem) {

  public SignatureProperties {
    algorithm = (algorithm == null || algorithm.isBlank()) ? "ED25519" : algorithm.trim();
    keyRef = (keyRef == null || keyRef.isBlank()) ? "SIGN_V1" : keyRef.trim();
    privateKeyPem = privateKeyPem == null ? "" : privateKeyPem.trim();
    publicKeyPem = publicKeyPem == null ? "" : publicKeyPem.trim();
  }
}
