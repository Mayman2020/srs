package com.gov.ac.correspondence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCorrespondenceCommentRequest(
    @NotBlank @Size(max = 20000) String body, Long parentCommentId) {}
