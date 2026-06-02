package com.gov.ac.feature.roles.controller;

import com.gov.ac.feature.roles.dto.RoleOptionDto;
import com.gov.ac.feature.roles.service.RoleService;
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
@PreAuthorize("@effectivePermission.has('ADMIN_ROLE_MANAGE')")
public class RoleController {

  private final RoleService roleService;

  @GetMapping
  public List<RoleOptionDto> list() {
    return roleService.listActiveOptions();
  }
}
