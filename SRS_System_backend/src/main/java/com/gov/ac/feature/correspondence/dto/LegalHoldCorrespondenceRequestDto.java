package com.gov.ac.feature.correspondence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LegalHoldCorrespondenceRequestDto(@NotBlank @Size(max = 2000) String reason) {}
