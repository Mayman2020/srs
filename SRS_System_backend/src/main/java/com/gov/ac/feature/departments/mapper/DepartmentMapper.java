package com.gov.ac.feature.departments.mapper;

import com.gov.ac.domain.org.Department;
import com.gov.ac.feature.departments.dto.DepartmentFlatDto;

public final class DepartmentMapper {

  private DepartmentMapper() {}

  public static DepartmentFlatDto toFlat(Department d) {
    Long parentId = d.getParent() != null ? d.getParent().getId() : null;
    return new DepartmentFlatDto(
        d.getId(), parentId, d.getCode(), d.getNameAr(), d.getNameEn(), d.getSortOrder());
  }
}
