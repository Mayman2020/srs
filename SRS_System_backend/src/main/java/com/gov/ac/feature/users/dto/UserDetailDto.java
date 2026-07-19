package com.gov.ac.feature.users.dto;

import com.gov.ac.common.audit.UserAuditRefDto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserDetailDto(
    UUID id,
    String username,
    String fullNameAr,
    String fullNameEn,
    String email,
    String departmentCode,
    Long departmentId,
    Boolean active,
    Boolean mustChangePassword,
    List<Long> roleIds,
    Long securityClearanceId,
    Instant createdAt,
    Instant updatedAt,
    UserAuditRefDto createdByUser,
    UserAuditRefDto updatedByUser) {}
