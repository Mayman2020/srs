package com.gov.ac.feature.communication.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CreateCircularRequestDto(
    @NotBlank String title,
    @NotBlank String body,
    @NotBlank String createdBy,
    boolean broadcast,
    List<String> recipientUserIds) {}
