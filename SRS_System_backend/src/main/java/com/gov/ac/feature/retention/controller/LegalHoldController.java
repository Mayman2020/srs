package com.gov.ac.feature.retention.controller;

import com.gov.ac.feature.retention.dto.LegalHoldDto;
import com.gov.ac.feature.retention.dto.LegalHoldPlaceRequestDto;
import com.gov.ac.feature.retention.dto.LegalHoldReleaseRequestDto;
import com.gov.ac.feature.retention.service.RetentionAdminService;
import com.gov.ac.security.SecurityUtils;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/retention/legal-holds")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class LegalHoldController {

  private final RetentionAdminService retentionAdminService;

  @GetMapping("/active")
  @PreAuthorize("@effectivePermission.has('LEGAL_HOLD_VIEW')")
  public List<LegalHoldDto> active() {
    return retentionAdminService.listActiveHolds();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("@effectivePermission.has('LEGAL_HOLD_MANAGE')")
  public LegalHoldDto place(@Valid @RequestBody LegalHoldPlaceRequestDto body) {
    return retentionAdminService.placeHold(SecurityUtils.requireCurrentUserId(), body);
  }

  @PostMapping("/{id}/release")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("@effectivePermission.has('LEGAL_HOLD_MANAGE')")
  public void release(
      @PathVariable UUID id, @Valid @RequestBody LegalHoldReleaseRequestDto body) {
    retentionAdminService.releaseHold(SecurityUtils.requireCurrentUserId(), id, body);
  }
}
