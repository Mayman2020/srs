package com.gov.ac.correspondence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CorrespondenceReplySendRequest(
    @NotBlank @Size(max = 500_000) String bodyHtml) {}
