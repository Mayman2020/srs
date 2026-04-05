package com.gov.ac.correspondence.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gov.ac.modules.audit.service.AuditTrailService;
import com.gov.ac.modules.audit.web.dto.CreateAuditEventRequest;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CorrespondenceActionAudit {

  public static final String ACTION_CANCEL = "CORRESPONDENCE_CANCELLED";
  public static final String ACTION_ATTACHMENT_ADD = "CORRESPONDENCE_ATTACHMENT_ADDED";
  public static final String ACTION_ATTACHMENT_DELETE = "CORRESPONDENCE_ATTACHMENT_DELETED";
  public static final String ACTION_DRAFT_SAVE = "CORRESPONDENCE_DRAFT_SAVED";
  public static final String ACTION_REPLY_SENT = "CORRESPONDENCE_REPLY_SENT";
  public static final String ACTION_NOTIFICATION_DELETE = "NOTIFICATION_DELETED";

  private final AuditTrailService auditTrailService;
  private final ObjectMapper objectMapper;

  public void log(UUID actorUserId, String actionCode, UUID correspondenceId, Map<String, Object> detail) {
    String json = toJson(detail);
    auditTrailService.append(
        new CreateAuditEventRequest(
            actorUserId.toString(),
            actionCode,
            "CORRESPONDENCE",
            correspondenceId.toString(),
            json,
            null,
            null,
            null));
  }

  public void logResource(
      UUID actorUserId, String actionCode, String resourceType, String resourceId, Map<String, Object> detail) {
    String json = toJson(detail);
    auditTrailService.append(
        new CreateAuditEventRequest(
            actorUserId.toString(), actionCode, resourceType, resourceId, json, null, null, null));
  }

  private String toJson(Map<String, Object> detail) {
    try {
      return objectMapper.writeValueAsString(detail);
    } catch (JsonProcessingException e) {
      log.warn("Audit detail serialization failed: {}", e.getMessage());
      return "{}";
    }
  }
}
