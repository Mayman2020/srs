package com.gov.ac.feature.lookups.admin.mapper;

import com.gov.ac.feature.lookups.admin.dto.LookupCatalogDto;
import com.gov.ac.feature.lookups.admin.dto.LookupRowAdminDto;
import com.gov.ac.feature.lookups.entity.ClassificationEntity;
import com.gov.ac.feature.lookups.entity.ConfidentialityEntity;
import com.gov.ac.feature.lookups.entity.CorrespondenceStatusEntity;
import com.gov.ac.feature.lookups.entity.CorrespondenceTypeEntity;
import com.gov.ac.feature.lookups.entity.LookupCatalogEntity;
import com.gov.ac.feature.lookups.entity.OrgVisualNodeStatusEntity;
import com.gov.ac.feature.lookups.entity.PriorityEntity;
import com.gov.ac.feature.lookups.entity.WorkflowActionTypeEntity;
import com.gov.ac.feature.lookups.entity.WorkflowHistoryEventTypeEntity;

public final class LookupTableAdminMapper {

  private LookupTableAdminMapper() {}

  public static LookupCatalogDto toCatalogDto(LookupCatalogEntity catalog) {
    return new LookupCatalogDto(
        catalog.getLookupCode(),
        catalog.getNameAr(),
        catalog.getNameEn(),
        catalog.getParentCatalog() != null ? catalog.getParentCatalog().getLookupCode() : null,
        catalog.getSortOrder());
  }

  public static LookupRowAdminDto mapType(CorrespondenceTypeEntity type, String lookupCode) {
    return new LookupRowAdminDto(
        type.getId(),
        lookupCode,
        type.getCode(),
        type.getNameAr(),
        type.getNameEn(),
        type.getDescription(),
        type.getSortOrder(),
        type.getActive(),
        null,
        null,
        null,
        null,
        null,
        null);
  }

  public static LookupRowAdminDto mapStatus(CorrespondenceStatusEntity status, String lookupCode) {
    return new LookupRowAdminDto(
        status.getId(),
        lookupCode,
        status.getCode(),
        status.getNameAr(),
        status.getNameEn(),
        status.getDescription(),
        status.getSortOrder(),
        status.getActive(),
        status.getCorrespondenceType() != null ? status.getCorrespondenceType().getId() : null,
        status.getTerminal(),
        null,
        null,
        null,
        status.getUiVariant());
  }

  public static LookupRowAdminDto mapPriority(PriorityEntity priority, String lookupCode) {
    return new LookupRowAdminDto(
        priority.getId(),
        lookupCode,
        priority.getCode(),
        priority.getNameAr(),
        priority.getNameEn(),
        priority.getDescription(),
        priority.getSortOrder(),
        priority.getActive(),
        null,
        null,
        priority.getSlaDays(),
        null,
        null,
        null);
  }

  public static LookupRowAdminDto mapConf(ConfidentialityEntity confidentiality, String lookupCode) {
    return new LookupRowAdminDto(
        confidentiality.getId(),
        lookupCode,
        confidentiality.getCode(),
        confidentiality.getNameAr(),
        confidentiality.getNameEn(),
        confidentiality.getDescription(),
        confidentiality.getSortOrder(),
        confidentiality.getActive(),
        null,
        null,
        null,
        confidentiality.getRestrictsExport(),
        confidentiality.getRequiresClearance(),
        null);
  }

  public static LookupRowAdminDto mapClassification(ClassificationEntity classification, String lookupCode) {
    return new LookupRowAdminDto(
        classification.getId(),
        lookupCode,
        classification.getCode(),
        classification.getNameAr(),
        classification.getNameEn(),
        classification.getDescription(),
        classification.getSortOrder(),
        classification.getActive(),
        classification.getParent() != null ? classification.getParent().getId() : null,
        null,
        null,
        null,
        null,
        null);
  }

  public static LookupRowAdminDto mapWfAction(WorkflowActionTypeEntity actionType, String lookupCode) {
    return new LookupRowAdminDto(
        actionType.getId(),
        lookupCode,
        actionType.getCode(),
        actionType.getNameAr(),
        actionType.getNameEn(),
        actionType.getDescription(),
        actionType.getSortOrder(),
        actionType.getActive(),
        null,
        null,
        null,
        null,
        null,
        null);
  }

  public static LookupRowAdminDto mapWfEvent(WorkflowHistoryEventTypeEntity eventType, String lookupCode) {
    return new LookupRowAdminDto(
        eventType.getId(),
        lookupCode,
        eventType.getCode(),
        eventType.getNameAr(),
        eventType.getNameEn(),
        eventType.getDescription(),
        eventType.getSortOrder(),
        eventType.getActive(),
        null,
        null,
        null,
        null,
        null,
        null);
  }

  public static LookupRowAdminDto mapOrgVisualNode(
      OrgVisualNodeStatusEntity visualNodeStatus, String lookupCode) {
    return new LookupRowAdminDto(
        visualNodeStatus.getId(),
        lookupCode,
        visualNodeStatus.getCode(),
        visualNodeStatus.getNameAr(),
        visualNodeStatus.getNameEn(),
        visualNodeStatus.getDescription(),
        visualNodeStatus.getSortOrder(),
        visualNodeStatus.getActive(),
        null,
        null,
        null,
        null,
        null,
        null);
  }
}
