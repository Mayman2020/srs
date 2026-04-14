package com.gov.ac.feature.workflow.routes.mapper;

import com.gov.ac.feature.lookups.entity.CorrespondenceTypeEntity;
import com.gov.ac.feature.workflow.routes.dto.ServiceWorkflowRouteDto;
import com.gov.ac.feature.workflow.routes.entity.ServiceWorkflowRouteEntity;

public final class ServiceWorkflowRouteMapper {

  private ServiceWorkflowRouteMapper() {}

  public static ServiceWorkflowRouteDto toDto(ServiceWorkflowRouteEntity route) {
    CorrespondenceTypeEntity type = route.getCorrespondenceType();
    return new ServiceWorkflowRouteDto(
        route.getId(),
        type != null ? type.getId() : 0L,
        type != null ? type.getCode() : "",
        route.getProcessDefinitionKey(),
        route.getNameAr(),
        route.getNameEn(),
        route.isDefaultRoute(),
        route.getSortOrder() != null ? route.getSortOrder() : 0,
        Boolean.TRUE.equals(route.getActive()));
  }
}
