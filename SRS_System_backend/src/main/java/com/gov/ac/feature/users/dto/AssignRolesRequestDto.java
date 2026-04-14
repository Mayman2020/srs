package com.gov.ac.feature.users.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AssignRolesRequestDto(@NotEmpty List<@NotNull Long> roleIds) {}
