package com.gov.ac.feature.lookups.service;

import com.gov.ac.feature.lookups.dto.LookupBundleDto;
import com.gov.ac.feature.lookups.dto.LookupItemDto;
import com.gov.ac.feature.lookups.mapper.LookupDtoMapper;
import com.gov.ac.persistence.ClassificationRepository;
import com.gov.ac.persistence.ConfidentialityRepository;
import com.gov.ac.persistence.CorrespondenceStatusRepository;
import com.gov.ac.persistence.CorrespondenceTypeRepository;
import com.gov.ac.persistence.PriorityRepository;
import com.gov.ac.persistence.WorkflowActionTypeRepository;
import com.gov.ac.persistence.WorkflowHistoryEventTypeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LookupService {

  private final CorrespondenceTypeRepository correspondenceTypeRepository;
  private final CorrespondenceStatusRepository correspondenceStatusRepository;
  private final PriorityRepository priorityRepository;
  private final ConfidentialityRepository confidentialityRepository;
  private final ClassificationRepository classificationRepository;
  private final WorkflowActionTypeRepository workflowActionTypeRepository;
  private final WorkflowHistoryEventTypeRepository workflowHistoryEventTypeRepository;

  @Transactional(readOnly = true)
  public LookupBundleDto bundle() {
    return new LookupBundleDto(
        LookupDtoMapper.mapTypes(
            correspondenceTypeRepository.findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc()),
        LookupDtoMapper.mapStatuses(
            correspondenceStatusRepository.findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc()),
        LookupDtoMapper.mapPriorities(
            priorityRepository.findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc()),
        LookupDtoMapper.mapConf(
            confidentialityRepository.findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc()),
        LookupDtoMapper.mapClassifications(
            classificationRepository.findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc()),
        LookupDtoMapper.mapWfActions(
            workflowActionTypeRepository.findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc()),
        LookupDtoMapper.mapWfHistoryEvents(
            workflowHistoryEventTypeRepository.findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc()));
  }

  @Transactional(readOnly = true)
  public List<LookupItemDto> priorityLookup() {
    return LookupDtoMapper.mapPriorities(
        priorityRepository.findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc());
  }
}
