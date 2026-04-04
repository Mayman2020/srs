package com.gov.ac.feature.reports.dto;

public record DepartmentSlaRowDto(
    long departmentId,
    String code,
    String nameAr,
    String nameEn,
    long totalCorrespondences,
    long overdueOpen) {}
