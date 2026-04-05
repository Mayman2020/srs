package com.gov.ac.feature.admin.controller;

import com.gov.ac.feature.admin.dto.PermissionDto;
import com.gov.ac.feature.admin.dto.ResolveSystemIssueRequest;
import com.gov.ac.feature.admin.dto.RolePermissionIdsRequest;
import com.gov.ac.feature.admin.dto.SystemIssueDto;
import com.gov.ac.feature.admin.dto.UiScreenDto;
import com.gov.ac.feature.admin.dto.UpsertPermissionRequest;
import com.gov.ac.feature.admin.dto.UpsertUiScreenRequest;
import com.gov.ac.feature.admin.service.AdminConsoleService;
import com.gov.ac.feature.admin.service.SystemIssueAdminService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("@rbacExpressions.canManageUsers(authentication)")
public class AdminConsoleController {

  private final AdminConsoleService adminConsoleService;
  private final SystemIssueAdminService systemIssueAdminService;

  @GetMapping("/permissions")
  public List<PermissionDto> listPermissions() {
    return adminConsoleService.listPermissions();
  }

  @PostMapping("/permissions")
  public PermissionDto createPermission(@Valid @RequestBody UpsertPermissionRequest body) {
    return adminConsoleService.createPermission(body);
  }

  @PutMapping("/permissions/{id}")
  public PermissionDto updatePermission(
      @PathVariable Long id, @Valid @RequestBody UpsertPermissionRequest body) {
    return adminConsoleService.updatePermission(id, body);
  }

  @DeleteMapping("/permissions/{id}")
  public void deletePermission(@PathVariable Long id) {
    adminConsoleService.deletePermission(id);
  }

  @GetMapping("/roles/{roleId}/permissions")
  public List<Long> listRolePermissionIds(@PathVariable Long roleId) {
    return adminConsoleService.listPermissionIdsForRole(roleId);
  }

  @PutMapping("/roles/{roleId}/permissions")
  public void replaceRolePermissions(
      @PathVariable Long roleId, @Valid @RequestBody RolePermissionIdsRequest body) {
    adminConsoleService.replaceRolePermissions(roleId, body.permissionIds());
  }

  @GetMapping("/ui-screens")
  public List<UiScreenDto> listUiScreens() {
    return adminConsoleService.listUiScreens();
  }

  @PostMapping("/ui-screens")
  public UiScreenDto createUiScreen(@Valid @RequestBody UpsertUiScreenRequest body) {
    return adminConsoleService.createUiScreen(body);
  }

  @PutMapping("/ui-screens/{id}")
  public UiScreenDto updateUiScreen(
      @PathVariable Long id, @Valid @RequestBody UpsertUiScreenRequest body) {
    return adminConsoleService.updateUiScreen(id, body);
  }

  @DeleteMapping("/ui-screens/{id}")
  public void deleteUiScreen(@PathVariable Long id) {
    adminConsoleService.deleteUiScreen(id);
  }

  @GetMapping("/system-issues")
  public List<SystemIssueDto> listSystemIssues() {
    return systemIssueAdminService.listRecent();
  }

  @PatchMapping("/system-issues/{id}/resolve")
  public SystemIssueDto resolveSystemIssue(
      @PathVariable Long id, @RequestBody(required = false) ResolveSystemIssueRequest body) {
    return systemIssueAdminService.resolve(id, body != null ? body : new ResolveSystemIssueRequest(null));
  }
}
