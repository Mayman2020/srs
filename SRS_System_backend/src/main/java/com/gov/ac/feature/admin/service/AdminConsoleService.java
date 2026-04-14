package com.gov.ac.feature.admin.service;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.feature.admin.entity.UiScreenEntity;
import com.gov.ac.feature.roles.entity.PermissionEntity;
import com.gov.ac.feature.roles.entity.RoleEntity;
import com.gov.ac.feature.roles.entity.RolePermissionEntity;
import com.gov.ac.feature.roles.entity.RolePermissionId;
import com.gov.ac.feature.admin.dto.PermissionDto;
import com.gov.ac.feature.admin.dto.UiScreenDto;
import com.gov.ac.feature.admin.dto.UpsertPermissionRequestDto;
import com.gov.ac.feature.admin.dto.UpsertUiScreenRequestDto;
import com.gov.ac.feature.admin.mapper.AdminConsoleMapper;
import com.gov.ac.feature.roles.repository.PermissionRepository;
import com.gov.ac.feature.roles.repository.RolePermissionRepository;
import com.gov.ac.feature.roles.repository.RoleRepository;
import com.gov.ac.feature.admin.repository.UiScreenRepository;
import com.gov.ac.security.SecurityUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminConsoleService {

  private final PermissionRepository permissionRepository;
  private final RoleRepository roleRepository;
  private final RolePermissionRepository rolePermissionRepository;
  private final UiScreenRepository uiScreenRepository;

  @Transactional(readOnly = true)
  public List<PermissionDto> listPermissions() {
    return permissionRepository.findByDeletedAtIsNullOrderBySortOrderAsc().stream()
        .map(AdminConsoleMapper::toPermissionDto)
        .toList();
  }

  @Transactional
  public PermissionDto createPermission(UpsertPermissionRequestDto req) {
    String code = req.code().trim();
    if (permissionRepository.existsByCodeIgnoreCaseAndDeletedAtIsNull(code)) {
      throw new BadRequestException("PermissionEntity code already exists");
    }
    UUID actor = SecurityUtils.requireCurrentUserId();
    PermissionEntity p = new PermissionEntity();
    p.setCode(code);
    p.setNameAr(req.nameAr().trim());
    p.setNameEn(req.nameEn().trim());
    p.setDescription(trimToNull(req.description()));
    p.setSortOrder(req.sortOrder());
    p.setActive(req.active());
    p.setUiScreenId(resolveUiScreenIdOrNull(req.uiScreenId()));
    p.setCreatedBy(actor);
    p.setUpdatedBy(actor);
    return AdminConsoleMapper.toPermissionDto(permissionRepository.save(p));
  }

  @Transactional
  public PermissionDto updatePermission(Long id, UpsertPermissionRequestDto req) {
    PermissionEntity p =
        permissionRepository
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("PermissionEntity not found"));
    String code = req.code().trim();
    if (permissionRepository.existsByCodeIgnoreCaseAndDeletedAtIsNullAndIdNot(code, id)) {
      throw new BadRequestException("PermissionEntity code already exists");
    }
    UUID actor = SecurityUtils.requireCurrentUserId();
    p.setCode(code);
    p.setNameAr(req.nameAr().trim());
    p.setNameEn(req.nameEn().trim());
    p.setDescription(trimToNull(req.description()));
    p.setSortOrder(req.sortOrder());
    p.setActive(req.active());
    p.setUiScreenId(resolveUiScreenIdOrNull(req.uiScreenId()));
    p.setUpdatedBy(actor);
    return AdminConsoleMapper.toPermissionDto(permissionRepository.save(p));
  }

  @Transactional
  public void deletePermission(Long id) {
    PermissionEntity p =
        permissionRepository
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("PermissionEntity not found"));
    UUID actor = SecurityUtils.requireCurrentUserId();
    p.setDeletedAt(java.time.Instant.now());
    p.setDeletedBy(actor);
    p.setUpdatedBy(actor);
    permissionRepository.save(p);
  }

  @Transactional(readOnly = true)
  public List<Long> listPermissionIdsForRole(Long roleId) {
    ensureRole(roleId);
    return rolePermissionRepository.findPermissionIdsByRoleId(roleId);
  }

  @Transactional
  public void replaceRolePermissions(Long roleId, List<Long> permissionIds) {
    RoleEntity role = ensureRole(roleId);
    SecurityUtils.requireCurrentUserId();
    rolePermissionRepository.deleteByRoleId(roleId);
    if (permissionIds == null || permissionIds.isEmpty()) {
      return;
    }
    List<RolePermissionEntity> rows = new ArrayList<>();
    for (Long pid : permissionIds.stream().distinct().toList()) {
      permissionRepository
          .findByIdAndDeletedAtIsNull(pid)
          .orElseThrow(() -> new NotFoundException("PermissionEntity not found: " + pid));
      RolePermissionEntity rp = new RolePermissionEntity();
      rp.setId(new RolePermissionId(role.getId(), pid));
      rows.add(rp);
    }
    rolePermissionRepository.saveAll(rows);
  }

  @Transactional(readOnly = true)
  public List<UiScreenDto> listUiScreens() {
    return uiScreenRepository.findByDeletedAtIsNullOrderBySortOrderAsc().stream()
        .map(AdminConsoleMapper::toUiScreenDto)
        .toList();
  }

  @Transactional
  public UiScreenDto createUiScreen(UpsertUiScreenRequestDto req) {
    String code = req.code().trim();
    if (uiScreenRepository.existsByCodeIgnoreCaseAndDeletedAtIsNull(code)) {
      throw new BadRequestException("Screen code already exists");
    }
    UUID actor = SecurityUtils.requireCurrentUserId();
    UiScreenEntity s = new UiScreenEntity();
    s.setCode(code);
    s.setRoutePath(req.routePath().trim());
    s.setNameAr(req.nameAr().trim());
    s.setNameEn(req.nameEn().trim());
    s.setDescription(trimToNull(req.description()));
    s.setSortOrder(req.sortOrder());
    s.setActive(req.active());
    s.setIconKey(normalizeIconKey(req.iconKey()));
    s.setShowInShellNav(Boolean.TRUE.equals(req.showInShellNav()));
    s.setRequiredPermissionId(req.requiredPermissionId());
    s.setCreatedBy(actor);
    s.setUpdatedBy(actor);
    return AdminConsoleMapper.toUiScreenDto(uiScreenRepository.save(s));
  }

  @Transactional
  public UiScreenDto updateUiScreen(Long id, UpsertUiScreenRequestDto req) {
    UiScreenEntity s =
        uiScreenRepository
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Screen not found"));
    String code = req.code().trim();
    if (uiScreenRepository.existsByCodeIgnoreCaseAndDeletedAtIsNullAndIdNot(code, id)) {
      throw new BadRequestException("Screen code already exists");
    }
    UUID actor = SecurityUtils.requireCurrentUserId();
    s.setCode(code);
    s.setRoutePath(req.routePath().trim());
    s.setNameAr(req.nameAr().trim());
    s.setNameEn(req.nameEn().trim());
    s.setDescription(trimToNull(req.description()));
    s.setSortOrder(req.sortOrder());
    s.setActive(req.active());
    s.setIconKey(normalizeIconKey(req.iconKey()));
    s.setShowInShellNav(Boolean.TRUE.equals(req.showInShellNav()));
    s.setRequiredPermissionId(req.requiredPermissionId());
    s.setUpdatedBy(actor);
    return AdminConsoleMapper.toUiScreenDto(uiScreenRepository.save(s));
  }

  @Transactional
  public void deleteUiScreen(Long id) {
    UiScreenEntity s =
        uiScreenRepository
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Screen not found"));
    UUID actor = SecurityUtils.requireCurrentUserId();
    s.setDeletedAt(java.time.Instant.now());
    s.setDeletedBy(actor);
    s.setUpdatedBy(actor);
    uiScreenRepository.save(s);
  }

  private RoleEntity ensureRole(Long roleId) {
    return roleRepository
        .findByIdAndDeletedAtIsNullAndActiveTrue(roleId)
        .orElseThrow(() -> new NotFoundException("RoleEntity not found"));
  }

  private Long resolveUiScreenIdOrNull(Long id) {
    if (id == null) {
      return null;
    }
    return uiScreenRepository
        .findByIdAndDeletedAtIsNull(id)
        .map(UiScreenEntity::getId)
        .orElseThrow(() -> new NotFoundException("UI screen not found"));
  }

  private static String normalizeIconKey(String raw) {
    if (!StringUtils.hasText(raw)) {
      return "apps";
    }
    return raw.trim();
  }

  private static String trimToNull(String s) {
    if (s == null) {
      return null;
    }
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }
}
