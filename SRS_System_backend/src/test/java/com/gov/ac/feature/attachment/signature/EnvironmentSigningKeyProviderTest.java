package com.gov.ac.feature.attachment.signature;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.MessageDigest;
import org.junit.jupiter.api.Test;

class EnvironmentSigningKeyProviderTest {

  @Test
  void ephemeralSignAndVerify() throws Exception {
    SignatureProperties props = new SignatureProperties("ED25519", "SIGN_V1", "", "");
    EnvironmentSigningKeyProvider provider = new EnvironmentSigningKeyProvider(props);

    byte[] hash = MessageDigest.getInstance("SHA-256").digest("payload".getBytes());
    SigningKeyProvider.SignatureResult result = provider.sign(hash);

    assertThat(result.algorithm()).isEqualTo("ED25519");
    assertThat(result.keyRef()).isEqualTo("SIGN_V1");
    assertThat(result.signatureBytes()).isNotEmpty();

    boolean ok = provider.verify(hash, result.signatureBytes(), result.keyRef(), result.algorithm());
    assertThat(ok).isTrue();

    byte[] tampered = hash.clone();
    tampered[0] ^= 0x01;
    assertThat(provider.verify(tampered, result.signatureBytes(), result.keyRef(), result.algorithm()))
        .isFalse();
  }

  @Test
  void verifyRejectsUnknownKeyRefOrAlgorithm() throws Exception {
    EnvironmentSigningKeyProvider provider =
        new EnvironmentSigningKeyProvider(new SignatureProperties("ED25519", "SIGN_V1", "", ""));
    byte[] hash = MessageDigest.getInstance("SHA-256").digest("x".getBytes());
    SigningKeyProvider.SignatureResult result = provider.sign(hash);

    assertThat(provider.verify(hash, result.signatureBytes(), "WRONG_REF", "ED25519")).isFalse();
    assertThat(provider.verify(hash, result.signatureBytes(), "SIGN_V1", "OTHER")).isFalse();
  }
}
