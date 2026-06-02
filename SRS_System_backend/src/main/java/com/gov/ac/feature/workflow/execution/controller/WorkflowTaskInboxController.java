package com.gov.ac.feature.workflow.execution.controller;

import com.gov.ac.feature.workflow.execution.dto.WorkflowTaskInboxRowDto;
import com.gov.ac.feature.workflow.execution.service.WorkflowTaskInboxService;
import com.gov.ac.security.SecurityUtils;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Caller-scoped workflow task inbox. Always uses the JWT subject, never a query parameter, to
 * prevent IDOR.
 */
@RestController
@RequestMapping("/api/v1/workflow/tasks")
@RequiredArgsConstructor
@PreAuthorize("@effectivePermission.has('CORRESPONDENCE_VIEW')")
public class WorkflowTaskInboxController {

  private final WorkflowTaskInboxService workflowTaskInboxService;

  @GetMapping("/inbox")
  public List<WorkflowTaskInboxRowDto> myInbox(
      @RequestParam(name = "limit", defaultValue = "100") int limit) {
    return workflowTaskInboxService.listMyOpenTasks(SecurityUtils.requireCurrentUserId(), limit);
  }
}
