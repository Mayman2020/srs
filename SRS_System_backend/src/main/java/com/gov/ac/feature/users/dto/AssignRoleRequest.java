package com.gov.ac.feature.users.dto;

import jakarta.validation.constraints.NotNull;

public record AssignRoleRequest(@NotNull Long roleId) {}
