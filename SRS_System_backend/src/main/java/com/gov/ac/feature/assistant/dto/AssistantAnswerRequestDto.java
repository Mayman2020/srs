package com.gov.ac.feature.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssistantAnswerRequestDto(
    @NotBlank @Size(max = 2000) String query) {}
