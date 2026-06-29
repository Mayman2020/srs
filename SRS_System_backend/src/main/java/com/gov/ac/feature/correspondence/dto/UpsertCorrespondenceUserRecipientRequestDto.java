package com.gov.ac.feature.correspondence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UpsertCorrespondenceUserRecipientRequestDto(
    @NotNull UUID recipientUserId,
    @NotBlank String recipientKindCode) {}
