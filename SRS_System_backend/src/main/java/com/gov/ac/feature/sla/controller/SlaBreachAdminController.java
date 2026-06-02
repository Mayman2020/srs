package com.gov.ac.feature.sla.controller;

import com.gov.ac.feature.sla.dto.SlaBreachEventDto;
import com.gov.ac.feature.sla.mapper.SlaPolicyMapper;
import com.gov.ac.feature.sla.repository.SlaBreachEventRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only access to the SLA breach ledger for ops dashboards and audit reviews. Authenticated
 * callers with {@code SLA_POLICY_VIEW} get the full feed; unresolved-only by default to keep the
 * UI focused on what still needs intervention.
 */
@RestController
@RequestMapping("/api/v1/admin/sla/breaches")
@RequiredArgsConstructor
@PreAuthorize("@effectivePermission.has('SLA_POLICY_VIEW')")
public class SlaBreachAdminController {

  private final SlaBreachEventRepository slaBreachEventRepository;

  @GetMapping
  @PreAuthorize("@effectivePermission.has('SLA_POLICY_VIEW')")
  public List<SlaBreachEventDto> list(
      @RequestParam(name = "onlyActive", defaultValue = "true") boolean onlyActive) {
    return slaBreachEventRepository.findRecent(onlyActive).stream()
        .map(SlaPolicyMapper::toBreachDto)
        .toList();
  }
}
