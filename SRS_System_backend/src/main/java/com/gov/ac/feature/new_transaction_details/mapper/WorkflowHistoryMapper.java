package com.gov.ac.feature.new_transaction_details.mapper;

import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.workflow.execution.entity.WorkflowHistoryEntity;
import com.gov.ac.feature.new_transaction_details.dto.WorkflowHistoryEntryDto;

public final class WorkflowHistoryMapper {

  private WorkflowHistoryMapper() {}

  public static WorkflowHistoryEntryDto toEntry(WorkflowHistoryEntity h) {
    AppUserEntity actor = h.getActor();
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
