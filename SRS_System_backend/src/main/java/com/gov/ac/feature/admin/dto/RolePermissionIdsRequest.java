package com.gov.ac.feature.admin.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RolePermissionIdsRequest(@NotNull List<Long> permissionIds) {}
