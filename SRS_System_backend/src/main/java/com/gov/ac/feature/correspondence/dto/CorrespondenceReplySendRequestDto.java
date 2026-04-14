package com.gov.ac.feature.correspondence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CorrespondenceReplySendRequestDto(
    @NotBlank @Size(max = 500_000) String bodyHtml) {}
