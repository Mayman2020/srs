package com.gov.ac.feature.notification.channel;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class WebhookSignatureHelperTest {

  @Test
  void hmacSha256Base64_isDeterministic() {
    byte[] body = "{\"a\":1}".getBytes(StandardCharsets.UTF_8);
    String s1 = WebhookSignatureHelper.hmacSha256Base64("secret", body);
    String s2 = WebhookSignatureHelper.hmacSha256Base64("secret", body);
    assertThat(s1).isEqualTo(s2).isNotBlank();
  }
}
