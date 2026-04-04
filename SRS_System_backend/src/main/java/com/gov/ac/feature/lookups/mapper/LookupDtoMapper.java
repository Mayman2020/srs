package com.gov.ac.feature.lookups.mapper;

import com.gov.ac.domain.lookup.Confidentiality;
import com.gov.ac.domain.lookup.CorrespondenceStatus;
import com.gov.ac.domain.lookup.CorrespondenceType;
import com.gov.ac.domain.lookup.Priority;
import com.gov.ac.domain.lookup.WorkflowActionType;
import com.gov.ac.domain.lookup.WorkflowHistoryEventType;
import com.gov.ac.domain.org.Classification;
import com.gov.ac.feature.lookups.dto.LookupItemDto;
import java.util.List;

public final class LookupDtoMapper {

  private LookupDtoMapper() {}

  public static List<LookupItemDto> mapTypes(List<CorrespondenceType> rows) {
    return rows.stream()
        .map(
            r ->
                new LookupItemDto(
                    r.getId(), r.getCode(), r.getNameAr(), r.getNameEn(), r.getSortOrder()))
        .toList();
  }

  public static List<LookupItemDto> mapStatuses(List<CorrespondenceStatus> rows) {
    return rows.stream()
        .map(
            r ->
                new LookupItemDto(
                    r.getId(), r.getCode(), r.getNameAr(), r.getNameEn(), r.getSortOrder()))
        .toList();
  }

  public static List<LookupItemDto> mapPriorities(List<Priority> rows) {
    return rows.stream()
        .map(
            r ->
                new LookupItemDto(
                    r.getId(), r.getCode(), r.getNameAr(), r.getNameEn(), r.getSortOrder()))
        .toList();
  }

  public static List<LookupItemDto> mapConf(List<Confidentiality> rows) {
    return rows.stream()
        .map(
            r ->
                new LookupItemDto(
                    r.getId(), r.getCode(), r.getNameAr(), r.getNameEn(), r.getSortOrder()))
        .toList();
  }

  public static List<LookupItemDto> mapClassifications(List<Classification> rows) {
    return rows.stream()
        .map(
            r ->
                new LookupItemDto(
                    r.getId(), r.getCode(), r.getNameAr(), r.getNameEn(), r.getSortOrder()))
        .toList();
  }

  public static List<LookupItemDto> mapWfActions(List<WorkflowActionType> rows) {
    return rows.stream()
        .map(
            r ->
                new LookupItemDto(
                    r.getId(), r.getCode(), r.getNameAr(), r.getNameEn(), r.getSortOrder()))
        .toList();
  }

  public static List<LookupItemDto> mapWfHistoryEvents(List<WorkflowHistoryEventType> rows) {
    return rows.stream()
        .map(
            r ->
                new LookupItemDto(
                    r.getId(), r.getCode(), r.getNameAr(), r.getNameEn(), r.getSortOrder()))
        .toList();
  }
}
