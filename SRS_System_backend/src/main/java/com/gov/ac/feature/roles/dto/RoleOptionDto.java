package com.gov.ac.feature.roles.dto;

public record RoleOptionDto(
    Long id, String code, String nameAr, String nameEn, Integer sortOrder, Long parentId) {}
