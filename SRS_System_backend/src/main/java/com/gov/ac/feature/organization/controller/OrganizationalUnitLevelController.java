package com.gov.ac.feature.organization.controller;

import com.gov.ac.feature.organization.dto.OrganizationalUnitLevelDto;
import com.gov.ac.feature.organization.service.OrganizationalUnitLevelService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only endpoint feeding the FE org-levels admin / chain-preview rendering. */
@RestController
@RequestMapping("/api/v1/organization/levels")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class OrganizationalUnitLevelController {

  private final OrganizationalUnitLevelService service;

  @GetMapping
  public List<OrganizationalUnitLevelDto> list() {
    return service.listActive();
  }
}
