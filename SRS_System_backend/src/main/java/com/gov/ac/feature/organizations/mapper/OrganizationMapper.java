package com.gov.ac.feature.organizations.mapper;

import com.gov.ac.feature.organizations.dto.OrganizationFlatDto;
import com.gov.ac.feature.organizations.entity.OrganizationEntity;

public final class OrganizationMapper {

  private OrganizationMapper() {}

  public static OrganizationFlatDto toFlat(OrganizationEntity organization) {
    return new OrganizationFlatDto(
        organization.getId(),
        organization.getParent() != null ? organization.getParent().getId() : null,
        organization.getCode(),
        organization.getNameAr(),
        organization.getNameEn(),
        Boolean.TRUE.equals(organization.getExternal()));
  }
}
