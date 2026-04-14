package com.gov.ac.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record SwitchRoleRequestDto(@NotBlank String roleCode) {}
