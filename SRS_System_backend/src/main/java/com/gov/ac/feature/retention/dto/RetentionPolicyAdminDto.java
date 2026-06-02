package com.gov.ac.feature.retention.dto;

import java.util.UUID;

public record RetentionPolicyAdminDto(
    UUID id,
    String code,
    String nameEn,
    String nameAr,
    String appliesTo,
    Integer retainForDays,
    String actionAfter,
    Boolean enabled) {}
