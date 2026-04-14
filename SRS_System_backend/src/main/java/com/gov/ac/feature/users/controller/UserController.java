package com.gov.ac.feature.users.controller;

import com.gov.ac.feature.users.dto.AssignRoleRequestDto;
import com.gov.ac.feature.users.dto.AssignRolesRequestDto;
import com.gov.ac.feature.users.dto.CreateAppUserRequestDto;
import com.gov.ac.feature.users.dto.UpdateAppUserRequestDto;
import com.gov.ac.feature.users.dto.UserDetailDto;
import com.gov.ac.feature.users.dto.UserListDto;
import com.gov.ac.feature.users.service.UserAdminService;
import com.gov.ac.security.SecurityUtils;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

/** Frontend: {@code features/users} */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("@effectivePermission.has(authentication, 'user.manage')")
public class UserController {

  private final UserAdminService userAdminService;

  @GetMapping
  public Page<UserListDto> page(
      @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return userAdminService.listUsers(pageable);
  }

  @GetMapping("/{userId}")
  public UserDetailDto getOne(@PathVariable UUID userId) {
    return userAdminService.getUserDetail(userId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public UserDetailDto create(@Valid @RequestBody CreateAppUserRequestDto body) {
    return userAdminService.createUser(SecurityUtils.requireCurrentUserId(), body);
  }

  @PutMapping("/{userId}")
  public UserDetailDto update(
      @PathVariable UUID userId, @Valid @RequestBody UpdateAppUserRequestDto body) {
    return userAdminService.updateUser(SecurityUtils.requireCurrentUserId(), userId, body);
  }

  @DeleteMapping("/{userId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID userId) {
    userAdminService.deleteUser(SecurityUtils.requireCurrentUserId(), userId);
  }

  @PostMapping("/{userId}/roles")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void assignRole(
      @PathVariable UUID userId, @Valid @RequestBody AssignRoleRequestDto body) {
    userAdminService.assignRole(SecurityUtils.requireCurrentUserId(), userId, body.roleId());
  }

  @PutMapping("/{userId}/roles")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void assignRoles(
      @PathVariable UUID userId, @Valid @RequestBody AssignRolesRequestDto body) {
    userAdminService.setRoles(SecurityUtils.requireCurrentUserId(), userId, body.roleIds());
  }
}
