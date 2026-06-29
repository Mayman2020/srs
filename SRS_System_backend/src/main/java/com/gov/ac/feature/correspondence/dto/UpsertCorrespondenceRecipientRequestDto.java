package com.gov.ac.feature.correspondence.dto;

import jakarta.validation.constraints.NotNull;

public record UpsertCorrespondenceRecipientRequestDto(@NotNull Long departmentId) {}
