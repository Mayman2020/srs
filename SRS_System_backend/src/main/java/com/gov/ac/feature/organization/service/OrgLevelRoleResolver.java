package com.gov.ac.feature.organization.service;

import com.gov.ac.feature.organization.repository.OrganizationalUnitLevelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Resolves default {@code role.code} for a Q/L/K/S level from {@code organizational_unit_level}. */
@Service
@RequiredArgsConstructor
public class OrgLevelRoleResolver {

  private final OrganizationalUnitLevelRepository organizationalUnitLevelRepository;

  @Transactional(readOnly = true)
  public String resolveRoleCode(String levelCode) {
    if (!StringUtils.hasText(levelCode)) {
      return fallbackRoleCode();
    }
    return organizationalUnitLevelRepository
        .findActiveByCode(levelCode.trim())
        .map(l -> l.getDefaultRoleCode())
        .filter(StringUtils::hasText)
        .orElseGet(this::fallbackRoleCode);
  }

  private String fallbackRoleCode() {
    return organizationalUnitLevelRepository
        .findActiveByCode("S")
        .map(l -> l.getDefaultRoleCode())
        .filter(StringUtils::hasText)
        .orElse("STAFF");
  }
}
