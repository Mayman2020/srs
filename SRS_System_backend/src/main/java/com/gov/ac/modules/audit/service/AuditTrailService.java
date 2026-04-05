package com.gov.ac.modules.audit.service;

import com.gov.ac.domain.audit.AuditEvent;
import com.gov.ac.modules.audit.web.dto.AuditEventRecord;
import com.gov.ac.modules.audit.web.dto.CreateAuditEventRequest;
import com.gov.ac.persistence.AuditEventRepository;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditTrailService {

  private final AuditEventRepository auditEventRepository;

  @Transactional
  public UUID append(CreateAuditEventRequest req) {
    AuditEvent e = new AuditEvent();
    e.setActorUserId(req.actorUserId().trim());
    e.setActionCode(req.actionCode().trim());
    e.setResourceType(req.resourceType());
    e.setResourceId(req.resourceId());
    e.setDetailJson(req.detailJson());
    e.setIpAddress(req.ipAddress());
    e.setUserAgent(req.userAgent());
    if (req.occurredAt() != null) {
      e.setOccurredAt(req.occurredAt());
    }
    return auditEventRepository.save(e).getId();
  }

  @Transactional(readOnly = true)
  public List<AuditEventRecord> query(
      String actor, String action, Instant from, Instant to, int limit) {
    Instant f = from != null ? from : Instant.EPOCH;
    Instant t = to != null ? to : Instant.now();
    return auditEventRepository.search(actor, action, f, t).stream()
        .limit(Math.min(Math.max(limit, 1), 500))
        .map(
            e ->
                new AuditEventRecord(
                    e.getId(),
                    e.getOccurredAt(),
                    e.getActorUserId(),
                    e.getActionCode(),
                    e.getResourceType(),
                    e.getResourceId(),
                    e.getDetailJson(),
                    e.getIpAddress(),
                    e.getUserAgent()))
        .toList();
  }
}
