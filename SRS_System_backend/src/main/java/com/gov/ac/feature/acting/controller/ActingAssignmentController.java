package com.gov.ac.feature.acting.controller;

import com.gov.ac.feature.acting.dto.ActingAssignmentDto;
import com.gov.ac.feature.acting.dto.ActingAssignmentListDto;
import com.gov.ac.feature.acting.dto.CreateActingAssignmentRequestDto;
import com.gov.ac.feature.acting.service.ActingAssignmentService;
import com.gov.ac.security.SecurityUtils;
import com.gov.ac.security.permission.EffectiveUserPermissionService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/acting-assignments")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ActingAssignmentController {

  private final ActingAssignmentService actingAssignmentService;
  private final EffectiveUserPermissionService effectiveUserPermissionService;

  @GetMapping("/mine")
  public ActingAssignmentListDto listMine() {
    return actingAssignmentService.listForUser(SecurityUtils.requireCurrentUserId());
  }

  @GetMapping("/audit")
  @PreAuthorize("@effectivePermission.has('ACTING_ASSIGNMENT_VIEW')")
  public List<ActingAssignmentDto> listAudit() {
    return actingAssignmentService.listAllForAudit();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(
      "@effectivePermission.has('ACTING_ASSIGNMENT_ADMIN') or @effectivePermission.has('ACTING_ASSIGNMENT_MANAGE_OWN')")
  public ActingAssignmentDto create(@Valid @RequestBody CreateActingAssignmentRequestDto body) {
    UUID actor = SecurityUtils.requireCurrentUserId();
    boolean asAdmin = effectiveUserPermissionService.hasActivePermission(actor, "ACTING_ASSIGNMENT_ADMIN");
    return actingAssignmentService.create(actor, body, asAdmin);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(
      "@effectivePermission.has('ACTING_ASSIGNMENT_ADMIN') or @effectivePermission.has('ACTING_ASSIGNMENT_MANAGE_OWN')")
  public void revoke(@PathVariable UUID id) {
    UUID actor = SecurityUtils.requireCurrentUserId();
    boolean asAdmin = effectiveUserPermissionService.hasActivePermission(actor, "ACTING_ASSIGNMENT_ADMIN");
    actingAssignmentService.revoke(actor, id, asAdmin);
  }
}
