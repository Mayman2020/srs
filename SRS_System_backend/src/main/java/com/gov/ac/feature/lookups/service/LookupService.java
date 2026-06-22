package com.gov.ac.feature.lookups.service;

import com.gov.ac.feature.lookups.dto.LookupBundleDto;
import com.gov.ac.feature.lookups.dto.LookupItemDto;
import com.gov.ac.feature.lookups.mapper.LookupDtoMapper;
import com.gov.ac.feature.shared.lookup.LookupCodes;
import com.gov.ac.feature.lookups.repository.ClassificationRepository;
import com.gov.ac.feature.lookups.repository.ConfidentialityRepository;
import com.gov.ac.feature.lookups.repository.CorrespondenceStatusRepository;
import com.gov.ac.feature.lookups.repository.CorrespondenceTypeRepository;
import com.gov.ac.feature.leave.repository.LeaveStatusRepository;
import com.gov.ac.feature.lookups.repository.LookupCatalogRepository;
import com.gov.ac.feature.lookups.repository.OrgVisualNodeStatusRepository;
import com.gov.ac.feature.lookups.repository.PriorityRepository;
import com.gov.ac.feature.lookups.repository.WorkflowActionTypeRepository;
import com.gov.ac.feature.lookups.repository.WorkflowHistoryEventTypeRepository;
import java.util.List;
import java.util.Locale;
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
  private final OrgVisualNodeStatusRepository orgVisualNodeStatusRepository;
  private final LeaveStatusRepository leaveStatusRepository;
  private final LookupCatalogRepository lookupCatalogRepository;

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
            workflowHistoryEventTypeRepository.findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc()),
        LookupDtoMapper.mapOrgVisualNodeStatuses(
            orgVisualNodeStatusRepository.findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc()));
  }

  @Transactional(readOnly = true)
  public List<LookupItemDto> priorityLookup() {
    return byLookupCode(LookupCodes.PRIORITY);
  }

  @Transactional(readOnly = true)
  public List<LookupItemDto> byLookupCode(String lookupCode) {
    String normalized = normalizeLookupCode(lookupCode);
    return switch (normalized) {
      case LookupCodes.CORRESPONDENCE_TYPE ->
          LookupDtoMapper.mapTypes(
              correspondenceTypeRepository.findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc());
      case LookupCodes.CORRESPONDENCE_STATUS ->
          LookupDtoMapper.mapStatuses(
              correspondenceStatusRepository
                  .findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc());
      case LookupCodes.PRIORITY ->
          LookupDtoMapper.mapPriorities(
              priorityRepository.findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc());
      case LookupCodes.CONFIDENTIALITY ->
          LookupDtoMapper.mapConf(
              confidentialityRepository.findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc());
      case LookupCodes.CLASSIFICATION ->
          LookupDtoMapper.mapClassifications(
              classificationRepository.findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc());
      case LookupCodes.WORKFLOW_ACTION_TYPE ->
          LookupDtoMapper.mapWfActions(
              workflowActionTypeRepository.findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc());
      case LookupCodes.WORKFLOW_HISTORY_EVENT_TYPE ->
          LookupDtoMapper.mapWfHistoryEvents(
              workflowHistoryEventTypeRepository
                  .findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc());
      case LookupCodes.ORG_VISUAL_NODE_STATUS ->
          LookupDtoMapper.mapOrgVisualNodeStatuses(
              orgVisualNodeStatusRepository
                  .findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc());
      case LookupCodes.LEAVE_STATUS ->
          LookupDtoMapper.mapLeaveStatuses(
              leaveStatusRepository.findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc());
      default -> byParentCode(normalized);
    };
  }

  @Transactional(readOnly = true)
  public List<LookupItemDto> byParentCode(String parentCode) {
    return LookupDtoMapper.mapCatalogItems(
        lookupCatalogRepository.findByParentCatalog_LookupCodeOrderBySortOrderAsc(
            normalizeLookupCode(parentCode)));
  }

  private static String normalizeLookupCode(String lookupCode) {
    return lookupCode == null ? "" : lookupCode.trim().toLowerCase(Locale.ROOT);
  }
}
