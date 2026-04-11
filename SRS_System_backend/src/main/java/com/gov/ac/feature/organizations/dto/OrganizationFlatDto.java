package com.gov.ac.feature.organizations.dto;

public record OrganizationFlatDto(
    long id, Long parentId, String code, String nameAr, String nameEn, boolean external) {}
