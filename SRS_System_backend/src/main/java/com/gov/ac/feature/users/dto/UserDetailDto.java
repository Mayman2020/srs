package com.gov.ac.feature.users.dto;

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
    List<Long> roleIds) {}
