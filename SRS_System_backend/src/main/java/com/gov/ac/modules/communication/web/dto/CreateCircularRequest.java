package com.gov.ac.modules.communication.web.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CreateCircularRequest(
    @NotBlank String title,
    @NotBlank String body,
    @NotBlank String createdBy,
    boolean broadcast,
    List<String> recipientUserIds) {}
