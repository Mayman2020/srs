package com.gov.ac.web;

import com.gov.ac.domain.user.AppUser;
import com.gov.ac.domain.workflow.WorkflowHistory;
import com.gov.ac.persistence.WorkflowHistoryRepository;
import com.gov.ac.web.dto.WorkflowHistoryEntryDto;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/correspondence/{correspondenceId}/workflow-history")
@RequiredArgsConstructor
public class WorkflowHistoryController {

  private final WorkflowHistoryRepository workflowHistoryRepository;

  @GetMapping
  public List<WorkflowHistoryEntryDto> timeline(@PathVariable UUID correspondenceId) {
    return workflowHistoryRepository.findByCorrespondence_IdOrderBySequenceNoAsc(correspondenceId).stream()
        .map(this::toDto)
        .toList();
  }

  private WorkflowHistoryEntryDto toDto(WorkflowHistory h) {
    AppUser actor = h.getActor();
    return new WorkflowHistoryEntryDto(
        h.getId(),
        h.getCorrespondence().getId(),
        h.getEventType().getCode(),
        h.getWorkflowActionType() != null ? h.getWorkflowActionType().getCode() : null,
        h.getWorkflowAction() != null ? h.getWorkflowAction().getId() : null,
        actor != null ? actor.getId() : null,
        actor != null ? actor.getFullNameAr() : null,
        h.getOccurredAt(),
        h.getSequenceNo(),
        h.getPrimaryCommentText(),
        h.getDetail(),
        h.getSlaDueAt(),
        h.getSlaBreachedAt(),
        h.getActualDurationMs(),
        h.getPreviousCorrespondenceStatus() != null
            ? h.getPreviousCorrespondenceStatus().getCode()
            : null,
        h.getNewCorrespondenceStatus() != null ? h.getNewCorrespondenceStatus().getCode() : null,
        h.getCamundaTaskId());
  }
}
