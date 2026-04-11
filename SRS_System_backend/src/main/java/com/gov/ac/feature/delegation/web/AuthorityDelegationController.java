package com.gov.ac.feature.delegation.web;

import com.gov.ac.feature.delegation.dto.AuthorityDelegationDto;
import com.gov.ac.feature.delegation.dto.CreateAuthorityDelegationRequest;
import com.gov.ac.feature.delegation.service.AuthorityDelegationService;
import com.gov.ac.security.SecurityUtils;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
public class AuthorityDelegationController {

  private final AuthorityDelegationService authorityDelegationService;

  @GetMapping
  public List<AuthorityDelegationDto> listMine() {
    return authorityDelegationService.listForCurrentUser(SecurityUtils.requireCurrentUserId());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AuthorityDelegationDto create(@Valid @RequestBody CreateAuthorityDelegationRequest body) {
    return authorityDelegationService.create(SecurityUtils.requireCurrentUserId(), body);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void revoke(@PathVariable UUID id) {
    String role = SecurityUtils.requireCurrentRoleCode();
    boolean mayRevokeAsManager = "SYS_ADMIN".equals(role) || "CORRESP_MGR".equals(role);
    authorityDelegationService.revoke(SecurityUtils.requireCurrentUserId(), id, mayRevokeAsManager);
  }
}
