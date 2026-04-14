package com.gov.ac.feature.audit.mapper;

import com.gov.ac.feature.audit.dto.AuditEventRecordDto;
import com.gov.ac.feature.audit.dto.CreateAuditEventRequestDto;
import com.gov.ac.feature.audit.entity.AuditEventEntity;

public final class AuditEventMapper {

  private AuditEventMapper() {}

  public static AuditEventEntity fromCreateRequest(CreateAuditEventRequestDto request) {
    AuditEventEntity event = new AuditEventEntity();
    event.setActorUserId(request.actorUserId().trim());
    event.setActionCode(request.actionCode().trim());
    event.setResourceType(request.resourceType());
    event.setResourceId(request.resourceId());
    event.setDetailJson(request.detailJson());
    event.setIpAddress(request.ipAddress());
    event.setUserAgent(request.userAgent());
    if (request.occurredAt() != null) {
      event.setOccurredAt(request.occurredAt());
    }
    return event;
  }

  public static AuditEventRecordDto toRecord(AuditEventEntity event) {
    return new AuditEventRecordDto(
        event.getId(),
        event.getOccurredAt(),
        event.getActorUserId(),
        event.getActionCode(),
        event.getResourceType(),
        event.getResourceId(),
        event.getDetailJson(),
        event.getIpAddress(),
        event.getUserAgent());
  }
}
