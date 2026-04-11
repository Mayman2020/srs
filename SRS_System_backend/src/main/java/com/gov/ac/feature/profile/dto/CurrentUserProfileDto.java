package com.gov.ac.feature.profile.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CurrentUserProfileDto(
    UUID id,
    String username,
    String fullNameAr,
    String fullNameEn,
    String email,
    String phone,
    String nationalId,
    Long departmentId,
    String departmentCode,
    String departmentNameAr,
    String departmentNameEn,
    Boolean active,
    Boolean mfaEnabled,
    Instant lastLoginAt,
    Instant passwordChangedAt,
    List<Long> roleIds,
    List<String> roleCodes,
    String profileImageUrl,
    String uiTheme,
    String uiLocale) {}
