package com.gov.ac.feature.attachment.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class EnvironmentKeyProviderTest {

  @Test
  void devFallbackRoundTrip() {
    EnvironmentKeyProvider provider =
        new EnvironmentKeyProvider(new AttachmentEncryptionProperties(true, "KEK_V1", ""));
    byte[] dek = new byte[32];
    for (int i = 0; i < dek.length; i++) {
      dek[i] = (byte) i;
    }
    KeyProvider.WrappedDek wrapped = provider.wrapDek(dek);
    assertThat(wrapped.keyRef()).isEqualTo("KEK_V1");
    byte[] unwrapped = provider.unwrapDek(wrapped.wrapped(), "KEK_V1");
    assertThat(unwrapped).containsExactly(dek);
  }

  @Test
  void hexKekRoundTrip() {
    byte[] kek = new byte[32];
    for (int i = 0; i < kek.length; i++) {
      kek[i] = (byte) (i * 7 + 1);
    }
    String hex = HexFormat.of().formatHex(kek);
    EnvironmentKeyProvider provider =
        new EnvironmentKeyProvider(new AttachmentEncryptionProperties(true, "KEK_V1", hex));
    byte[] dek = new byte[32];
    KeyProvider.WrappedDek wrapped = provider.wrapDek(dek);
    assertThat(provider.unwrapDek(wrapped.wrapped(), "KEK_V1")).containsExactly(dek);
  }

  @Test
  void rejectsUnknownKeyRef() {
    EnvironmentKeyProvider provider =
        new EnvironmentKeyProvider(new AttachmentEncryptionProperties(true, "KEK_V1", ""));
    byte[] dek = new byte[32];
    KeyProvider.WrappedDek wrapped = provider.wrapDek(dek);
    assertThatThrownBy(() -> provider.unwrapDek(wrapped.wrapped(), "OTHER"))
        .isInstanceOf(CryptoOperationException.class);
  }

  @Test
  void rejectsMalformedHex() {
    assertThatThrownBy(
            () ->
                new EnvironmentKeyProvider(
                    new AttachmentEncryptionProperties(true, "KEK_V1", "0123")))
        .isInstanceOf(CryptoOperationException.class);
  }
}
