package com.gov.ac.web;

import com.gov.ac.domain.lookup.Confidentiality;
import com.gov.ac.domain.lookup.CorrespondenceStatus;
import com.gov.ac.domain.lookup.CorrespondenceType;
import com.gov.ac.domain.lookup.Priority;
import com.gov.ac.domain.lookup.WorkflowActionType;
import com.gov.ac.domain.lookup.WorkflowHistoryEventType;
import com.gov.ac.persistence.ConfidentialityRepository;
import com.gov.ac.persistence.CorrespondenceStatusRepository;
import com.gov.ac.persistence.CorrespondenceTypeRepository;
import com.gov.ac.persistence.PriorityRepository;
import com.gov.ac.persistence.WorkflowActionTypeRepository;
import com.gov.ac.persistence.WorkflowHistoryEventTypeRepository;
import com.gov.ac.web.dto.LookupBundleDto;
import com.gov.ac.web.dto.LookupItemDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lookups")
@RequiredArgsConstructor
public class LookupController {

  private final CorrespondenceTypeRepository correspondenceTypeRepository;
  private final CorrespondenceStatusRepository correspondenceStatusRepository;
  private final PriorityRepository priorityRepository;
  private final ConfidentialityRepository confidentialityRepository;
  private final WorkflowActionTypeRepository workflowActionTypeRepository;
  private final WorkflowHistoryEventTypeRepository workflowHistoryEventTypeRepository;

  @GetMapping
  public LookupBundleDto bundle() {
    return new LookupBundleDto(
        mapTypes(correspondenceTypeRepository.findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc()),
        mapStatuses(correspondenceStatusRepository.findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc()),
        mapPriorities(priorityRepository.findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc()),
        mapConf(confidentialityRepository.findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc()),
        mapWfActions(
            workflowActionTypeRepository.findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc()),
        mapWfHistoryEvents(
            workflowHistoryEventTypeRepository.findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc()));
  }

  /** Convenience endpoint; same rows as `bundle().priorities()`. */
  @GetMapping("/priority")
  public List<LookupItemDto> priorityLookup() {
    return mapPriorities(priorityRepository.findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc());
  }

  private static List<LookupItemDto> mapTypes(List<CorrespondenceType> rows) {
    return rows.stream()
        .map(
            r ->
                new LookupItemDto(
                    r.getId(), r.getCode(), r.getNameAr(), r.getNameEn(), r.getSortOrder()))
        .toList();
  }

  private static List<LookupItemDto> mapStatuses(List<CorrespondenceStatus> rows) {
    return rows.stream()
        .map(
            r ->
                new LookupItemDto(
                    r.getId(), r.getCode(), r.getNameAr(), r.getNameEn(), r.getSortOrder()))
        .toList();
  }

  private static List<LookupItemDto> mapPriorities(List<Priority> rows) {
    return rows.stream()
        .map(
            r ->
                new LookupItemDto(
                    r.getId(), r.getCode(), r.getNameAr(), r.getNameEn(), r.getSortOrder()))
        .toList();
  }

  private static List<LookupItemDto> mapConf(List<Confidentiality> rows) {
    return rows.stream()
        .map(
            r ->
                new LookupItemDto(
                    r.getId(), r.getCode(), r.getNameAr(), r.getNameEn(), r.getSortOrder()))
        .toList();
  }

  private static List<LookupItemDto> mapWfActions(List<WorkflowActionType> rows) {
    return rows.stream()
        .map(
            r ->
                new LookupItemDto(
                    r.getId(), r.getCode(), r.getNameAr(), r.getNameEn(), r.getSortOrder()))
        .toList();
  }

  private static List<LookupItemDto> mapWfHistoryEvents(List<WorkflowHistoryEventType> rows) {
    return rows.stream()
        .map(
            r ->
                new LookupItemDto(
                    r.getId(), r.getCode(), r.getNameAr(), r.getNameEn(), r.getSortOrder()))
        .toList();
  }
}
