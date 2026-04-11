package com.gov.ac.feature.workflow;

import com.gov.ac.feature.workflow.dto.ServiceWorkflowRouteDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workflow-routes")
@RequiredArgsConstructor
public class WorkflowRouteCatalogController {

  private final ServiceWorkflowRouteService serviceWorkflowRouteService;

  /** Active routes for a correspondence type (e.g. manual workflow selection on create). */
  @GetMapping
  public List<ServiceWorkflowRouteDto> listForType(
      @RequestParam("correspondenceTypeCode") String correspondenceTypeCode) {
    return serviceWorkflowRouteService.listActiveForTypeCode(correspondenceTypeCode);
  }
}
