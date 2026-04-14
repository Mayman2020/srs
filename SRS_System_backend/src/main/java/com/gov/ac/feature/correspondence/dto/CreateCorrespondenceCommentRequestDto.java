package com.gov.ac.feature.correspondence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCorrespondenceCommentRequestDto(
    @NotBlank @Size(max = 20000) String body, Long parentCommentId) {}
