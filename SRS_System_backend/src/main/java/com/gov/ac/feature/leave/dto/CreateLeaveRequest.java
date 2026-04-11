package com.gov.ac.feature.leave.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateLeaveRequest(
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    String reason) {}
