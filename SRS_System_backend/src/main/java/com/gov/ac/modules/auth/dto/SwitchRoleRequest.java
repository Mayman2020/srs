package com.gov.ac.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record SwitchRoleRequest(@NotBlank String roleCode) {}
