package com.gov.ac.feature.delegation.controller;

import com.gov.ac.feature.delegation.dto.AuthorityDelegationDto;
import com.gov.ac.feature.delegation.dto.CreateAuthorityDelegationRequestDto;
import com.gov.ac.feature.delegation.service.AuthorityDelegationService;
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
@RequestMapping("/api/v1/authority-delegations")
@RequiredArgsConstructor
@PreAuthorize("@effectivePermission.has('DELEGATION_MANAGE')")
public class AuthorityDelegationController {

  private final AuthorityDelegationService authorityDelegationService;
  private final EffectiveUserPermissionService effectiveUserPermissionService;

  @GetMapping
  public List<AuthorityDelegationDto> listMine() {
    return authorityDelegationService.listForCurrentUser(SecurityUtils.requireCurrentUserId());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AuthorityDelegationDto create(@Valid @RequestBody CreateAuthorityDelegationRequestDto body) {
    return authorityDelegationService.create(SecurityUtils.requireCurrentUserId(), body);
  }

  /**
   * Managers (anyone with {@code ADMIN_USER_MANAGE}) may revoke another user's delegation;
   * regular users may only revoke their own. The old implementation compared role codes against
   * hardcoded strings — replaced with a permission lookup so the rule survives renames.
   */
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void revoke(@PathVariable UUID id) {
    UUID userId = SecurityUtils.requireCurrentUserId();
    boolean mayRevokeAsManager =
        effectiveUserPermissionService.hasActivePermission(userId, "ADMIN_USER_MANAGE");
    authorityDelegationService.revoke(userId, id, mayRevokeAsManager);
  }
}
