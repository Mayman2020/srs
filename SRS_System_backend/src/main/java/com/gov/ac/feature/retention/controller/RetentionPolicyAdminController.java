package com.gov.ac.feature.retention.controller;

import com.gov.ac.feature.retention.dto.RetentionPolicyAdminDto;
import com.gov.ac.feature.retention.dto.RetentionPolicyToggleRequestDto;
import com.gov.ac.feature.retention.service.RetentionAdminService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/retention/policies")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class RetentionPolicyAdminController {

  private final RetentionAdminService retentionAdminService;

  @GetMapping
  @PreAuthorize("@effectivePermission.has('RETENTION_POLICY_VIEW')")
  public List<RetentionPolicyAdminDto> list() {
    return retentionAdminService.listPolicies();
  }

  @PatchMapping("/{id}/enabled")
  @PreAuthorize("@effectivePermission.has('RETENTION_POLICY_MANAGE')")
  public RetentionPolicyAdminDto toggle(
      @PathVariable UUID id, @Valid @RequestBody RetentionPolicyToggleRequestDto body) {
    return retentionAdminService.togglePolicy(id, body);
  }
}
