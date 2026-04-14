package com.gov.ac.feature.users.dto;

import jakarta.validation.constraints.NotNull;

public record AssignRoleRequestDto(@NotNull Long roleId) {}
