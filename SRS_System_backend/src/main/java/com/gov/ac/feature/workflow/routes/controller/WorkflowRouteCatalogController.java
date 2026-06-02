package com.gov.ac.feature.workflow.routes.controller;

import com.gov.ac.feature.workflow.routes.dto.ServiceWorkflowRouteDto;
import com.gov.ac.feature.workflow.routes.service.ServiceWorkflowRouteService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workflow-routes")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class WorkflowRouteCatalogController {

  private final ServiceWorkflowRouteService serviceWorkflowRouteService;

  /** Active routes for a correspondence type (e.g. manual workflow selection on create). */
  @GetMapping
  public List<ServiceWorkflowRouteDto> listForType(
      @RequestParam("correspondenceTypeCode") String correspondenceTypeCode) {
    return serviceWorkflowRouteService.listActiveForTypeCode(correspondenceTypeCode);
  }
}
