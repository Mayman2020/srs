package com.gov.ac.modules.audit.web;

import com.gov.ac.modules.audit.service.AuditTrailService;
import com.gov.ac.modules.audit.web.dto.AuditEventRecord;
import com.gov.ac.modules.audit.web.dto.CreateAuditEventRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

  private final AuditTrailService auditTrailService;

  @PostMapping("/events")
  public Map<String, String> create(@Valid @RequestBody CreateAuditEventRequest body) {
    UUID id = auditTrailService.append(body);
    return Map.of("id", id.toString());
  }

  @GetMapping("/events")
  public List<AuditEventRecord> list(
      @RequestParam(required = false) String actor,
      @RequestParam(required = false) String action,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant to,
      @RequestParam(defaultValue = "100") int limit) {
    return auditTrailService.query(actor, action, from, to, limit);
  }
}
