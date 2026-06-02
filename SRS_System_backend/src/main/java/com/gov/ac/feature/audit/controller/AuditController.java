package com.gov.ac.feature.audit.controller;

import com.gov.ac.feature.audit.service.AuditTrailService;
import com.gov.ac.feature.audit.dto.AuditEventRecordDto;
import com.gov.ac.feature.audit.dto.CreateAuditEventRequestDto;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
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

  /**
   * Any authenticated caller may append a client-side audit event for their own action (the
   * service stamps the actor from the JWT). No special permission is required.
   */
  @PostMapping("/events")
  @PreAuthorize("isAuthenticated()")
  public Map<String, String> create(@Valid @RequestBody CreateAuditEventRequestDto body) {
    UUID id = auditTrailService.append(body);
    return Map.of("id", id.toString());
  }

  @GetMapping("/events")
  @PreAuthorize("@effectivePermission.has('ADMIN_AUDIT_VIEW')")
  public List<AuditEventRecordDto> list(
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
