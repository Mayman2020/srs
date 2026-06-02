package com.gov.ac.feature.sla.controller;

import com.gov.ac.feature.sla.dto.SlaTaskStatusDto;
import com.gov.ac.feature.sla.service.SlaTaskStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Per-task SLA status read API. Any authenticated user can ask about a task they can already see
 * in their inbox (the inbox query is itself permission-gated). No additional capability is
 * required because the returned payload contains policy + countdown data, not protected
 * correspondence content.
 */
@RestController
@RequestMapping("/api/v1/sla/tasks")
@RequiredArgsConstructor
public class SlaTaskStatusController {

  private final SlaTaskStatusService slaTaskStatusService;

  @GetMapping("/{taskId}/status")
  @PreAuthorize("isAuthenticated()")
  public SlaTaskStatusDto getStatus(@PathVariable String taskId) {
    return slaTaskStatusService.getStatusForTask(taskId);
  }
}
