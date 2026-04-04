package com.gov.ac.feature.departments.dto;

public record DepartmentFlatDto(
    long id, Long parentId, String code, String nameAr, String nameEn, int sortOrder) {}
