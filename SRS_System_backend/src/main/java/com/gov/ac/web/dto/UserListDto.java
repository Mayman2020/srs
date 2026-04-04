package com.gov.ac.web.dto;

import java.util.UUID;

public record UserListDto(
    UUID id,
    String username,
    String fullNameAr,
    String fullNameEn,
    String email,
    String departmentCode,
    Boolean active) {}
