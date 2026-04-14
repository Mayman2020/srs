package com.gov.ac.feature.audit.service;

import com.gov.ac.feature.audit.entity.AuditEventEntity;
import com.gov.ac.feature.audit.dto.AuditEventRecordDto;
import com.gov.ac.feature.audit.dto.CreateAuditEventRequestDto;
import com.gov.ac.feature.audit.mapper.AuditEventMapper;
import com.gov.ac.feature.audit.repository.AuditEventRepository;
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
  public UUID append(CreateAuditEventRequestDto req) {
    AuditEventEntity event = AuditEventMapper.fromCreateRequest(req);
    return auditEventRepository.save(event).getId();
  }

  @Transactional(readOnly = true)
  public List<AuditEventRecordDto> query(
      String actor, String action, Instant from, Instant to, int limit) {
    Instant f = from != null ? from : Instant.EPOCH;
    Instant t = to != null ? to : Instant.now();
    return auditEventRepository.search(actor, action, f, t).stream()
        .limit(Math.min(Math.max(limit, 1), 500))
        .map(AuditEventMapper::toRecord)
        .toList();
  }
}
