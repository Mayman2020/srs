package com.gov.ac.feature.assistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gov.ac.config.AssistantProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class AssistantLlmClient {

  private final AssistantProperties properties;
  private final ObjectMapper objectMapper;

  public String complete(String systemPrompt, String userPrompt) {
    if (!properties.isLlmEnabled() || !StringUtils.hasText(properties.getLlmApiKey())) {
      return null;
    }
    try {
      String body =
          objectMapper.writeValueAsString(
              Map.of(
                  "model",
                  properties.getLlmModel(),
                  "temperature",
                  0.2,
                  "messages",
                  List.of(
                      Map.of("role", "system", "content", systemPrompt),
                      Map.of("role", "user", "content", userPrompt))));
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(properties.getLlmApiUrl()))
              .timeout(Duration.ofSeconds(properties.getLlmTimeoutSeconds()))
              .header("Authorization", "Bearer " + properties.getLlmApiKey().trim())
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(body))
              .build();
      HttpResponse<String> response =
          HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        log.warn("assistant LLM HTTP {}: {}", response.statusCode(), response.body());
        return null;
      }
      JsonNode root = objectMapper.readTree(response.body());
      JsonNode content = root.path("choices").path(0).path("message").path("content");
      return content.isTextual() ? content.asText().trim() : null;
    } catch (Exception ex) {
      log.warn("assistant LLM call failed: {}", ex.getMessage());
      return null;
    }
  }
}
