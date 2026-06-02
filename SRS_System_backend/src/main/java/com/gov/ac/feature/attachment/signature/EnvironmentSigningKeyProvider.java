package com.gov.ac.feature.attachment.signature;

import com.gov.ac.feature.attachment.crypto.CryptoOperationException;
import jakarta.annotation.PostConstruct;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Default {@link SigningKeyProvider}. Reads PEM-encoded ED25519 keys from
 * {@code ac.signature.private-key-pem} / {@code ac.signature.public-key-pem} (env vars
 * {@code AC_SIGN_PRIVATE_KEY_PEM} / {@code AC_SIGN_PUBLIC_KEY_PEM}). If either is missing the
 * provider generates an ephemeral keypair and logs WARN at startup — same posture as the JWT
 * secret. Production must always provision the keys.
 */
@Component
@ConditionalOnMissingBean(value = SigningKeyProvider.class, ignored = EnvironmentSigningKeyProvider.class)
@Slf4j
public class EnvironmentSigningKeyProvider implements SigningKeyProvider {

  private static final String ALGORITHM = "Ed25519";

  private final SignatureProperties properties;
  private final PrivateKey privateKey;
  private final PublicKey publicKey;
  private final String publicKeyPem;
  private final boolean ephemeral;

  public EnvironmentSigningKeyProvider(SignatureProperties properties) {
    this.properties = properties;
    boolean haveBoth = !properties.privateKeyPem().isEmpty() && !properties.publicKeyPem().isEmpty();
    if (haveBoth) {
      try {
        this.privateKey = parsePrivate(properties.privateKeyPem());
        this.publicKey = parsePublic(properties.publicKeyPem());
        this.publicKeyPem = properties.publicKeyPem();
        this.ephemeral = false;
      } catch (GeneralSecurityException e) {
        throw new CryptoOperationException("Failed to load ac.signature.* keys", e);
      }
    } else {
      try {
        KeyPairGenerator gen = KeyPairGenerator.getInstance(ALGORITHM);
        KeyPair pair = gen.generateKeyPair();
        this.privateKey = pair.getPrivate();
        this.publicKey = pair.getPublic();
        this.publicKeyPem = encodePublic(this.publicKey);
        this.ephemeral = true;
      } catch (GeneralSecurityException e) {
        throw new CryptoOperationException("Failed to generate ED25519 keypair", e);
      }
    }
  }

  @PostConstruct
  void announce() {
    if (ephemeral) {
      log.warn(
          "[crypto] ac.signature.* keys not configured — using an EPHEMERAL ED25519 keypair. "
              + "Signatures will not survive restart. Configure AC_SIGN_PRIVATE_KEY_PEM/AC_SIGN_PUBLIC_KEY_PEM in production.");
    } else {
      log.info("[crypto] EnvironmentSigningKeyProvider initialized with key ref {} (algorithm={})",
          properties.keyRef(), properties.algorithm());
    }
  }

  @Override
  public String algorithm() {
    return properties.algorithm();
  }

  @Override
  public String currentKeyRef() {
    return properties.keyRef();
  }

  @Override
  public String publicKeyPem() {
    return publicKeyPem;
  }

  @Override
  public SignatureResult sign(byte[] hash) {
    if (hash == null) {
      throw new CryptoOperationException("hash bytes are required");
    }
    try {
      Signature sig = Signature.getInstance(ALGORITHM);
      sig.initSign(privateKey);
      sig.update(hash);
      byte[] bytes = sig.sign();
      return new SignatureResult(properties.algorithm(), bytes, properties.keyRef(), null);
    } catch (GeneralSecurityException e) {
      throw new CryptoOperationException("Failed to sign hash", e);
    }
  }

  @Override
  public boolean verify(byte[] hash, byte[] signatureBytes, String keyRef, String algorithm) {
    if (hash == null || signatureBytes == null) {
      return false;
    }
    if (algorithm != null && !algorithm.equalsIgnoreCase(properties.algorithm())) {
      return false;
    }
    if (keyRef != null && !keyRef.equals(properties.keyRef())) {
      // Historical key refs would be looked up here; the env provider only knows the current ref
      // so any historic key is treated as unverifiable rather than throwing.
      return false;
    }
    try {
      Signature sig = Signature.getInstance(ALGORITHM);
      sig.initVerify(publicKey);
      sig.update(hash);
      return sig.verify(signatureBytes);
    } catch (GeneralSecurityException e) {
      return false;
    }
  }

  private static PrivateKey parsePrivate(String pem) throws GeneralSecurityException {
    byte[] der = decodePem(pem, "PRIVATE KEY");
    return KeyFactory.getInstance(ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(der));
  }

  private static PublicKey parsePublic(String pem) throws GeneralSecurityException {
    byte[] der = decodePem(pem, "PUBLIC KEY");
    return KeyFactory.getInstance(ALGORITHM).generatePublic(new X509EncodedKeySpec(der));
  }

  private static byte[] decodePem(String pem, String label) {
    String stripped =
        pem.replace("-----BEGIN " + label + "-----", "")
            .replace("-----END " + label + "-----", "")
            .replaceAll("\\s+", "");
    return Base64.getDecoder().decode(stripped);
  }

  private static String encodePublic(PublicKey key) {
    String b64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(key.getEncoded());
    return "-----BEGIN PUBLIC KEY-----\n" + b64 + "\n-----END PUBLIC KEY-----\n";
  }
}
