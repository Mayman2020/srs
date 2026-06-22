package com.gov.ac.feature.notification.dispatch.service;

import com.gov.ac.feature.notification.channel.TerminalNotificationDispatchException;
import com.gov.ac.feature.notification.dispatch.SmsGatewayProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboundSmsService {

  private final SmsGatewayProperties smsGatewayProperties;
  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  public void send(String phoneE164, String message) {
    try {
      sendOrThrow(phoneE164, message);
    } catch (Exception e) {
      log.warn("SMS send failed: {}", e.getMessage());
    }
  }

  /** Used by the notification outbox worker; propagates failures for retry / terminal handling. */
  public void sendOrThrow(String phoneE164, String message) throws Exception {
    String url = smsGatewayProperties.getGatewayUrl();
    if (url == null || url.isBlank()) {
      log.info("SMS (dev) -> {} : {}", phoneE164, message);
      return;
    }
    String payload =
        "{\"phone\":\"" + escapeJson(phoneE164) + "\",\"message\":\"" + escapeJson(message) + "\"}";
    HttpRequest.Builder builder =
        HttpRequest.newBuilder()
            .uri(URI.create(url.trim()))
            .timeout(Duration.ofSeconds(20))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8));
    String apiKey = smsGatewayProperties.getApiKey();
    if (apiKey != null && !apiKey.isBlank()) {
      builder.header(smsGatewayProperties.getApiKeyHeader(), apiKey);
    }
    HttpResponse<String> response =
        httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new TerminalNotificationDispatchException(
          "SMS gateway HTTP " + response.statusCode() + ": " + response.body());
    }
  }

  private static String escapeJson(String s) {
    if (s == null) {
      return "";
    }
    return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
  }
}
