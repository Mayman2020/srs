package com.gov.ac.feature.new_transaction_details.service;

import com.gov.ac.feature.new_transaction_details.dto.WorkflowHistoryEntryDto;
import com.gov.ac.feature.new_transaction_details.mapper.WorkflowHistoryMapper;
import com.gov.ac.persistence.WorkflowHistoryRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkflowHistoryService {

  private final WorkflowHistoryRepository workflowHistoryRepository;

  @Transactional(readOnly = true)
  public List<WorkflowHistoryEntryDto> timelineForCorrespondence(UUID correspondenceId) {
    return workflowHistoryRepository
        .findByCorrespondence_IdOrderBySequenceNoAsc(correspondenceId)
        .stream()
        .map(WorkflowHistoryMapper::toEntry)
        .toList();
  }
}
