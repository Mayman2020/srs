package com.gov.ac.feature.sla.controller;

import com.gov.ac.feature.sla.dto.CreateSlaPolicyRequestDto;
import com.gov.ac.feature.sla.dto.SlaPolicyDto;
import com.gov.ac.feature.sla.service.SlaPolicyManagementService;
import com.gov.ac.security.SecurityUtils;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin CRUD for SLA policies. Class-level {@code isAuthenticated()} ensures every method is
 * authenticated, with method-level checks adding canonical permission gates: {@code
 * SLA_POLICY_VIEW} for reads, {@code SLA_POLICY_MANAGE} for mutations.
 */
@RestController
@RequestMapping("/api/v1/admin/sla/policies")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class SlaPolicyAdminController {

  private final SlaPolicyManagementService slaPolicyManagementService;

  @GetMapping
  @PreAuthorize("@effectivePermission.has('SLA_POLICY_VIEW')")
  public List<SlaPolicyDto> list() {
    return slaPolicyManagementService.list();
  }

  @GetMapping("/{id}")
  @PreAuthorize("@effectivePermission.has('SLA_POLICY_VIEW')")
  public SlaPolicyDto get(@PathVariable Long id) {
    return slaPolicyManagementService.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("@effectivePermission.has('SLA_POLICY_MANAGE')")
  public SlaPolicyDto create(@Valid @RequestBody CreateSlaPolicyRequestDto body) {
    return slaPolicyManagementService.create(body, SecurityUtils.requireCurrentUserId());
  }

  @PutMapping("/{id}")
  @PreAuthorize("@effectivePermission.has('SLA_POLICY_MANAGE')")
  public SlaPolicyDto update(
      @PathVariable Long id, @Valid @RequestBody CreateSlaPolicyRequestDto body) {
    return slaPolicyManagementService.update(id, body, SecurityUtils.requireCurrentUserId());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("@effectivePermission.has('SLA_POLICY_MANAGE')")
  public void delete(@PathVariable Long id) {
    slaPolicyManagementService.delete(id, SecurityUtils.requireCurrentUserId());
  }
}
