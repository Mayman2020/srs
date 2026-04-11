package com.gov.ac.feature.lookups.admin.dto;

public record LookupCatalogDto(
    String lookupCode,
    String nameAr,
    String nameEn,
    String parentLookupCode,
    int sortOrder) {}
