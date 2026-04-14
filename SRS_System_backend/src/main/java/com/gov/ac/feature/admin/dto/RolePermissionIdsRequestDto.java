package com.gov.ac.feature.admin.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RolePermissionIdsRequestDto(@NotNull List<Long> permissionIds) {}
