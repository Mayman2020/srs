package com.gov.ac.feature.delegation.task.controller;

import com.gov.ac.feature.delegation.task.dto.CreateTaskDelegationRequestDto;
import com.gov.ac.feature.delegation.task.dto.TaskDelegationDto;
import com.gov.ac.feature.delegation.task.dto.TaskDelegationListDto;
import com.gov.ac.feature.delegation.task.service.TaskDelegationService;
import com.gov.ac.security.SecurityUtils;
import com.gov.ac.security.permission.EffectiveUserPermissionService;
import jakarta.validation.Valid;
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

/**
 * Task-level delegation API. Distinct from {@code /api/v1/authority-delegations} so existing
 * clients keep working; both endpoint families coexist by design.
 *
 * <p>Self-service create/list is gated by {@code TASK_DELEGATION_MANAGE_OWN}. Revoke uses an
 * inline check: any caller may revoke their own outgoing row, while {@code TASK_DELEGATION_ADMIN}
 * holders may revoke on someone else's behalf.
 */
@RestController
@RequestMapping("/api/v1/delegations/tasks")
@RequiredArgsConstructor
public class TaskDelegationController {

  private final TaskDelegationService taskDelegationService;
  private final EffectiveUserPermissionService effectiveUserPermissionService;

  @GetMapping("/mine")
  @PreAuthorize("isAuthenticated()")
  public TaskDelegationListDto listMine() {
    return taskDelegationService.listForUser(SecurityUtils.requireCurrentUserId());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("@effectivePermission.has('TASK_DELEGATION_MANAGE_OWN')")
  public TaskDelegationDto create(@Valid @RequestBody CreateTaskDelegationRequestDto body) {
    return taskDelegationService.create(SecurityUtils.requireCurrentUserId(), body);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("@effectivePermission.has('TASK_DELEGATION_MANAGE_OWN')")
  public void revoke(@PathVariable UUID id) {
    UUID userId = SecurityUtils.requireCurrentUserId();
    boolean mayRevokeAsAdmin =
        effectiveUserPermissionService.hasActivePermission(userId, "TASK_DELEGATION_ADMIN");
    taskDelegationService.revoke(userId, id, mayRevokeAsAdmin);
  }
}
