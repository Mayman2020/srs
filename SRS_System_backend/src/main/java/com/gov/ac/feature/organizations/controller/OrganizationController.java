package com.gov.ac.feature.organizations.controller;

import com.gov.ac.feature.organizations.dto.OrganizationFlatDto;
import com.gov.ac.feature.organizations.dto.UpsertOrganizationRequestDto;
import com.gov.ac.feature.organizations.service.OrganizationService;
import com.gov.ac.security.SecurityUtils;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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

/** External / government org tree for «الهيكل» screens (parallel to {@code /api/v1/departments}). */
@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class OrganizationController {

  private final OrganizationService organizationService;

  @GetMapping
  public List<OrganizationFlatDto> list() {
    return organizationService.listFlat();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("@effectivePermission.has('ADMIN_ORG_MANAGE')")
  public OrganizationFlatDto create(@Valid @RequestBody UpsertOrganizationRequestDto request) {
    UUID actor = SecurityUtils.requireCurrentUserId();
    return organizationService.create(actor, request);
  }

  @PutMapping("/{id}")
  @PreAuthorize("@effectivePermission.has('ADMIN_ORG_MANAGE')")
  public OrganizationFlatDto update(
      @PathVariable long id, @Valid @RequestBody UpsertOrganizationRequestDto request) {
    UUID actor = SecurityUtils.requireCurrentUserId();
    return organizationService.update(actor, id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("@effectivePermission.has('ADMIN_ORG_MANAGE')")
  public void delete(@PathVariable long id) {
    UUID actor = SecurityUtils.requireCurrentUserId();
    organizationService.delete(actor, id);
  }
}
