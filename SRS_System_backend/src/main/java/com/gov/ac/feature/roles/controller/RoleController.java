package com.gov.ac.feature.roles.controller;

import com.gov.ac.feature.lookups.dto.LookupItemDto;
import com.gov.ac.feature.users.service.UserAdminService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Frontend: {@code features/roles} */
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@PreAuthorize("@effectivePermission.has(authentication, 'role.manage')")
public class RoleController {

  private final UserAdminService userAdminService;

  @GetMapping
  public List<LookupItemDto> list() {
    return userAdminService.listRoles();
  }
}
