package com.gov.ac.feature.notification.dispatch;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ac.notification.sms")
@Getter
@Setter
public class SmsGatewayProperties {

  /** When empty, SMS is logged only (local dev). Production sets AC_SMS_GATEWAY_URL. */
  private String gatewayUrl = "";

  private String apiKeyHeader = "X-Api-Key";

  private String apiKey = "";
}
