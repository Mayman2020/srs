package com.gov.ac.feature.users.controller;

import com.gov.ac.feature.users.dto.AssignRoleRequest;
import com.gov.ac.feature.users.dto.UserListDto;
import com.gov.ac.feature.users.service.UserAdminService;
import com.gov.ac.security.SecurityUtils;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Frontend: {@code features/users} */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("@rbacExpressions.canManageUsers(authentication)")
public class UserController {

  private final UserAdminService userAdminService;

  @GetMapping
  public Page<UserListDto> page(@PageableDefault(size = 50) Pageable pageable) {
    return userAdminService.listUsers(pageable);
  }

  @PostMapping("/{userId}/roles")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void assignRole(
      @PathVariable UUID userId, @Valid @RequestBody AssignRoleRequest body) {
    userAdminService.assignRole(SecurityUtils.requireCurrentUserId(), userId, body.roleId());
  }
}
