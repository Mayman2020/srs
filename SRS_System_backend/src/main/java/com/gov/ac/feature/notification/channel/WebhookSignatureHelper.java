package com.gov.ac.feature.notification.channel;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.experimental.UtilityClass;

@UtilityClass
public class WebhookSignatureHelper {

  public static String hmacSha256Base64(String secret, byte[] body) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] sig = mac.doFinal(body);
      return Base64.getEncoder().encodeToString(sig);
    } catch (Exception e) {
      throw new IllegalStateException("HMAC-SHA256 failed", e);
    }
  }
}
