package com.gov.ac.feature.organization.controller;

import com.gov.ac.feature.organization.dto.RoutingChainDto;
import com.gov.ac.feature.organization.service.OrgRoutingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only API powering the FE routing-preview widget on the Create Correspondence screen.
 *
 * <p>Returns a deterministic {@link RoutingChainDto} computed from {@code department.level_code};
 * the FE uses it to render the chain before submission and the workflow start delegate calls the
 * same service to persist {@code workflow_instance.routing_chain_json}.
 */
@RestController
@RequestMapping("/api/v1/organization/routing")
@RequiredArgsConstructor
@PreAuthorize("@effectivePermission.has('CORRESPONDENCE_CREATE')")
public class OrgRoutingController {

  private final OrgRoutingService orgRoutingService;

  @GetMapping("/preview")
  public RoutingChainDto preview(
      @RequestParam("from") Long originatorDepartmentId,
      @RequestParam("to") Long targetDepartmentId) {
    return orgRoutingService.computeChain(originatorDepartmentId, targetDepartmentId);
  }
}
