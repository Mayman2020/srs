package com.gov.ac.feature.correspondence.dto;

import jakarta.validation.constraints.Size;

public record CorrespondenceCancelRequestDto(@Size(max = 2000) String reason) {}
