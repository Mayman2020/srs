package com.gov.ac.feature.lookups.mapper;

import com.gov.ac.feature.lookups.entity.ConfidentialityEntity;
import com.gov.ac.feature.lookups.entity.CorrespondenceStatusEntity;
import com.gov.ac.feature.lookups.entity.CorrespondenceTypeEntity;
import com.gov.ac.feature.lookups.entity.LookupCatalogEntity;
import com.gov.ac.feature.lookups.entity.OrgVisualNodeStatusEntity;
import com.gov.ac.feature.lookups.entity.PriorityEntity;
import com.gov.ac.feature.lookups.entity.WorkflowActionTypeEntity;
import com.gov.ac.feature.lookups.entity.WorkflowHistoryEventTypeEntity;
import com.gov.ac.feature.lookups.entity.ClassificationEntity;
import com.gov.ac.feature.lookups.dto.LookupItemDto;
import java.util.List;

public final class LookupDtoMapper {

  private LookupDtoMapper() {}

  public static List<LookupItemDto> mapTypes(List<CorrespondenceTypeEntity> rows) {
    return rows.stream()
        .map(
            r ->
                new LookupItemDto(
                    r.getId(),
                    r.getCode(),
                    r.getNameAr(),
                    r.getNameEn(),
                    r.getSortOrder(),
                    null,
                    null,
                    null,
                    r.getDashboardInboundHighlight(),
                    r.getDashboardOutboundHighlight(),
                    null))
        .toList();
  }

  public static List<LookupItemDto> mapStatuses(List<CorrespondenceStatusEntity> rows) {
    return rows.stream()
        .map(
            r ->
                new LookupItemDto(
                    r.getId(),
                    r.getCode(),
                    r.getNameAr(),
                    r.getNameEn(),
                    r.getSortOrder(),
                    r.getCorrespondenceType() != null ? r.getCorrespondenceType().getId() : null,
                    r.getTerminal(),
                    r.getKpiSegment(),
                    null,
                    null,
                    r.getUiVariant()))
        .toList();
  }

  public static List<LookupItemDto> mapPriorities(List<PriorityEntity> rows) {
    return rows.stream()
        .map(
            r ->
                new LookupItemDto(
                    r.getId(), r.getCode(), r.getNameAr(), r.getNameEn(), r.getSortOrder(), null))
        .toList();
  }

  public static List<LookupItemDto> mapConf(List<ConfidentialityEntity> rows) {
    return rows.stream()
        .map(
            r ->
                new LookupItemDto(
                    r.getId(), r.getCode(), r.getNameAr(), r.getNameEn(), r.getSortOrder(), null))
        .toList();
  }

  public static List<LookupItemDto> mapClassifications(List<ClassificationEntity> rows) {
    return rows.stream()
        .map(
            r ->
                new LookupItemDto(
                    r.getId(),
                    r.getCode(),
                    r.getNameAr(),
                    r.getNameEn(),
                    r.getSortOrder(),
                    r.getParent() != null ? r.getParent().getId() : null))
        .toList();
  }

  public static List<LookupItemDto> mapWfActions(List<WorkflowActionTypeEntity> rows) {
    return rows.stream()
        .map(
            r ->
                new LookupItemDto(
                    r.getId(), r.getCode(), r.getNameAr(), r.getNameEn(), r.getSortOrder(), null))
        .toList();
  }

  public static List<LookupItemDto> mapWfHistoryEvents(List<WorkflowHistoryEventTypeEntity> rows) {
    return rows.stream()
        .map(
            r ->
                new LookupItemDto(
                    r.getId(), r.getCode(), r.getNameAr(), r.getNameEn(), r.getSortOrder(), null))
        .toList();
  }

  public static List<LookupItemDto> mapOrgVisualNodeStatuses(List<OrgVisualNodeStatusEntity> rows) {
    return rows.stream()
        .map(
            r ->
                new LookupItemDto(
                    r.getId(), r.getCode(), r.getNameAr(), r.getNameEn(), r.getSortOrder(), null))
        .toList();
  }

  public static List<LookupItemDto> mapCatalogItems(List<LookupCatalogEntity> rows) {
    return rows.stream()
        .map(
            r ->
                new LookupItemDto(
                    null,
                    r.getLookupCode(),
                    r.getNameAr(),
                    r.getNameEn(),
                    r.getSortOrder(),
                    r.getParentCatalog() != null ? null : null))
        .toList();
  }
}
