package com.gov.ac.feature.notification.dispatch.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailDispatchRequestDto(
    @NotBlank @Email String to, @NotBlank String subject, @NotBlank String body) {}
