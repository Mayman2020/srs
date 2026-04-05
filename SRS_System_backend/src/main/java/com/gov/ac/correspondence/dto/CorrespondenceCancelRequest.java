package com.gov.ac.correspondence.dto;

import jakarta.validation.constraints.Size;

public record CorrespondenceCancelRequest(@Size(max = 2000) String reason) {}
