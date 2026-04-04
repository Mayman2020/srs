package com.gov.ac.feature.new_transaction_details.controller;

import com.gov.ac.feature.new_transaction_details.dto.WorkflowHistoryEntryDto;
import com.gov.ac.feature.new_transaction_details.service.WorkflowHistoryService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Frontend: {@code features/new_transaction_details} — workflow timeline. */
@RestController
@RequestMapping("/api/v1/correspondence/{correspondenceId}/workflow-history")
@RequiredArgsConstructor
public class WorkflowHistoryController {

  private final WorkflowHistoryService workflowHistoryService;

  @GetMapping
  public List<WorkflowHistoryEntryDto> timeline(@PathVariable UUID correspondenceId) {
    return workflowHistoryService.timelineForCorrespondence(correspondenceId);
  }
}
