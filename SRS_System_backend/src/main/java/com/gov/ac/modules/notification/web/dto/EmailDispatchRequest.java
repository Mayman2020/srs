package com.gov.ac.modules.notification.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailDispatchRequest(
    @NotBlank @Email String to, @NotBlank String subject, @NotBlank String body) {}
