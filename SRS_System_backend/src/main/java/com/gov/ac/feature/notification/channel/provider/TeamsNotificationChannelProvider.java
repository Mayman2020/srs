package com.gov.ac.feature.notification.channel.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gov.ac.feature.notification.channel.NotificationOutboxService;
import com.gov.ac.feature.notification.channel.NotificationWebhookProperties;
import com.gov.ac.feature.notification.channel.TerminalNotificationDispatchException;
import com.gov.ac.feature.notification.channel.WebhookSignatureHelper;
import com.gov.ac.feature.notification.channel.entity.NotificationChannelTargetEntity;
import com.gov.ac.feature.notification.channel.entity.NotificationOutboxEntity;
import com.gov.ac.feature.notification.channel.repository.NotificationChannelTargetRepository;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class TeamsNotificationChannelProvider implements NotificationChannelProvider {

  private final NotificationChannelTargetRepository targetRepository;
  private final NotificationWebhookProperties webhookProperties;
  private final Environment environment;
  private final ObjectMapper objectMapper;

  private final RestClient restClient = RestClient.create();

  @Override
  public String code() {
    return NotificationOutboxService.CHANNEL_TEAMS;
  }

  @Override
  public boolean supports(NotificationOutboxEntity row) {
    return NotificationOutboxService.CHANNEL_TEAMS.equalsIgnoreCase(row.getChannelCode());
  }

  @Override
  public void dispatch(NotificationOutboxEntity row) throws Exception {
    String payload = row.getPayloadJson();
    if (payload == null || payload.isBlank()) {
      throw new TerminalNotificationDispatchException("TEAMS missing payload_json");
    }
    JsonNode root = objectMapper.readTree(payload);
    String targetCode = readTargetCode(root, row);
    if (targetCode == null || targetCode.isBlank()) {
      throw new TerminalNotificationDispatchException("TEAMS missing targetCode in payload");
    }
    NotificationChannelTargetEntity target =
        targetRepository
            .findByChannelCodeAndTargetCodeAndDeletedAtIsNull(
                NotificationOutboxService.CHANNEL_TEAMS, targetCode)
            .orElseThrow(
                () ->
                    new TerminalNotificationDispatchException(
                        "TEAMS unknown targetCode=" + targetCode));
    if (!Boolean.TRUE.equals(target.getEnabled())) {
      throw new TerminalNotificationDispatchException("TEAMS target disabled: " + targetCode);
    }
    String url = target.getTargetUrl();
    if (url == null || url.isBlank()) {
      throw new TerminalNotificationDispatchException("TEAMS missing target_url");
    }
    String cardJson = buildMessageCard(row, root);
    byte[] bodyBytes = cardJson.getBytes(StandardCharsets.UTF_8);
    String secret = resolveSigningSecret(target);
    String signature = WebhookSignatureHelper.hmacSha256Base64(secret, bodyBytes);
    try {
      restClient
          .post()
          .uri(url)
          .contentType(MediaType.APPLICATION_JSON)
          .header("X-AC-Signature", "hmac-sha256=" + signature)
          .body(cardJson)
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientResponseException ex) {
      HttpStatusCode st = ex.getStatusCode();
      if (st.is5xxServerError() || st.value() == 408 || st.value() == 429) {
        throw ex;
      }
      if (st.is4xxClientError()) {
        throw new TerminalNotificationDispatchException(
            "TEAMS HTTP " + st.value() + ": " + ex.getMessage(), ex);
      }
      throw ex;
    }
  }

  private String buildMessageCard(NotificationOutboxEntity row, JsonNode inner) {
    ObjectNode card = objectMapper.createObjectNode();
    card.put("@type", "MessageCard");
    card.put("@context", "https://schema.org/extensions");
    card.put("summary", "Admin Communications notification");
    card.put("themeColor", "0078D4");
    ObjectNode section = card.putArray("sections").addObject();
    section.put("activityTitle", row.getEventTypeCode());
    ArrayNode facts = section.putArray("facts");
    addFact(facts, "messageKey", row.getMessageKey());
    if (row.getCorrelationResourceType() != null) {
      addFact(facts, "resourceType", row.getCorrelationResourceType());
    }
    if (row.getCorrelationResourceId() != null) {
      addFact(facts, "resourceId", row.getCorrelationResourceId());
    }
    section.put("text", inner.toPrettyString());
    return card.toString();
  }

  private static void addFact(ArrayNode facts, String name, String value) {
    if (value == null) {
      return;
    }
    ObjectNode f = facts.addObject();
    f.put("name", name);
    f.put("value", value);
  }

  private String readTargetCode(JsonNode root, NotificationOutboxEntity row) {
    if (root.hasNonNull("targetCode")) {
      return root.get("targetCode").asText();
    }
    String corr = row.getCorrelationResourceId();
    if (corr != null && corr.startsWith("TARGET:")) {
      return corr.substring("TARGET:".length());
    }
    return null;
  }

  private String resolveSigningSecret(NotificationChannelTargetEntity target) {
    String ref = target.getSigningSecretRef();
    if (ref != null && !ref.isBlank()) {
      String fromEnv = environment.getProperty(ref);
      if (fromEnv != null && !fromEnv.isBlank()) {
        return fromEnv;
      }
      String getenv = System.getenv(ref);
      if (getenv != null && !getenv.isBlank()) {
        return getenv;
      }
    }
    return Optional.ofNullable(webhookProperties.signingSecret()).orElse("");
  }
}
