package com.gov.ac.feature.admin.controller;

import com.gov.ac.feature.admin.dto.PermissionDto;
import com.gov.ac.feature.admin.dto.ResolveSystemIssueRequestDto;
import com.gov.ac.feature.admin.dto.RolePermissionIdsRequestDto;
import com.gov.ac.feature.admin.dto.SystemIssueDto;
import com.gov.ac.feature.admin.dto.UiScreenDto;
import com.gov.ac.feature.admin.dto.UpsertPermissionRequestDto;
import com.gov.ac.feature.admin.dto.UpsertUiScreenRequestDto;
import com.gov.ac.feature.admin.service.AdminConsoleService;
import com.gov.ac.feature.admin.service.SystemIssueAdminService;
import com.gov.ac.feature.workflow.routes.dto.ServiceWorkflowRouteDto;
import com.gov.ac.feature.workflow.routes.dto.UpsertServiceWorkflowRouteRequestDto;
import com.gov.ac.feature.workflow.routes.service.ServiceWorkflowRouteService;
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
public class AdminConsoleController {

  private final AdminConsoleService adminConsoleService;
  private final SystemIssueAdminService systemIssueAdminService;
  private final ServiceWorkflowRouteService serviceWorkflowRouteService;

  @GetMapping("/permissions")
  @PreAuthorize("@effectivePermission.has('ADMIN_ROLE_MANAGE') or @effectivePermission.has('ADMIN_UI_SCREEN_MANAGE')")
  public List<PermissionDto> listPermissions() {
    return adminConsoleService.listPermissions();
  }

  @PostMapping("/permissions")
  @PreAuthorize("@effectivePermission.has('ADMIN_ROLE_MANAGE')")
  public PermissionDto createPermission(@Valid @RequestBody UpsertPermissionRequestDto body) {
    return adminConsoleService.createPermission(body);
  }

  @PutMapping("/permissions/{id}")
  @PreAuthorize("@effectivePermission.has('ADMIN_ROLE_MANAGE')")
  public PermissionDto updatePermission(
      @PathVariable Long id, @Valid @RequestBody UpsertPermissionRequestDto body) {
    return adminConsoleService.updatePermission(id, body);
  }

  @DeleteMapping("/permissions/{id}")
  @PreAuthorize("@effectivePermission.has('ADMIN_ROLE_MANAGE')")
  public void deletePermission(@PathVariable Long id) {
    adminConsoleService.deletePermission(id);
  }

  @GetMapping("/roles/{roleId}/permissions")
  @PreAuthorize("@effectivePermission.has('ADMIN_ROLE_MANAGE')")
  public List<Long> listRolePermissionIds(@PathVariable Long roleId) {
    return adminConsoleService.listPermissionIdsForRole(roleId);
  }

  @PutMapping("/roles/{roleId}/permissions")
  @PreAuthorize("@effectivePermission.has('ADMIN_ROLE_MANAGE')")
  public void replaceRolePermissions(
      @PathVariable Long roleId, @Valid @RequestBody RolePermissionIdsRequestDto body) {
    adminConsoleService.replaceRolePermissions(roleId, body.permissionIds());
  }

  @GetMapping("/ui-screens")
  @PreAuthorize("@effectivePermission.has('ADMIN_UI_SCREEN_MANAGE')")
  public List<UiScreenDto> listUiScreens() {
    return adminConsoleService.listUiScreens();
  }

  @PostMapping("/ui-screens")
  @PreAuthorize("@effectivePermission.has('ADMIN_UI_SCREEN_MANAGE')")
  public UiScreenDto createUiScreen(@Valid @RequestBody UpsertUiScreenRequestDto body) {
    return adminConsoleService.createUiScreen(body);
  }

  @PutMapping("/ui-screens/{id}")
  @PreAuthorize("@effectivePermission.has('ADMIN_UI_SCREEN_MANAGE')")
  public UiScreenDto updateUiScreen(
      @PathVariable Long id, @Valid @RequestBody UpsertUiScreenRequestDto body) {
    return adminConsoleService.updateUiScreen(id, body);
  }

  @DeleteMapping("/ui-screens/{id}")
  @PreAuthorize("@effectivePermission.has('ADMIN_UI_SCREEN_MANAGE')")
  public void deleteUiScreen(@PathVariable Long id) {
    adminConsoleService.deleteUiScreen(id);
  }

  @GetMapping("/system-issues")
  @PreAuthorize("@effectivePermission.has('ADMIN_AUDIT_VIEW')")
  public List<SystemIssueDto> listSystemIssues() {
    return systemIssueAdminService.listRecent();
  }

  @PatchMapping("/system-issues/{id}/resolve")
  @PreAuthorize("@effectivePermission.has('ADMIN_AUDIT_VIEW')")
  public SystemIssueDto resolveSystemIssue(
      @PathVariable Long id, @RequestBody(required = false) ResolveSystemIssueRequestDto body) {
    return systemIssueAdminService.resolve(id, body != null ? body : new ResolveSystemIssueRequestDto(null));
  }

  @GetMapping("/workflow-routes")
  @PreAuthorize("@effectivePermission.has('ADMIN_ORG_MANAGE')")
  public List<ServiceWorkflowRouteDto> listWorkflowRoutes() {
    return serviceWorkflowRouteService.listAllForAdmin();
  }

  @PostMapping("/workflow-routes")
  @PreAuthorize("@effectivePermission.has('ADMIN_ORG_MANAGE')")
  public ServiceWorkflowRouteDto createWorkflowRoute(
      @Valid @RequestBody UpsertServiceWorkflowRouteRequestDto body) {
    return serviceWorkflowRouteService.create(body);
  }

  @PutMapping("/workflow-routes/{id}")
  @PreAuthorize("@effectivePermission.has('ADMIN_ORG_MANAGE')")
  public ServiceWorkflowRouteDto updateWorkflowRoute(
      @PathVariable long id, @Valid @RequestBody UpsertServiceWorkflowRouteRequestDto body) {
    return serviceWorkflowRouteService.update(id, body);
  }

  @DeleteMapping("/workflow-routes/{id}")
  @PreAuthorize("@effectivePermission.has('ADMIN_ORG_MANAGE')")
  public void deleteWorkflowRoute(@PathVariable long id) {
    serviceWorkflowRouteService.delete(id);
  }
}
