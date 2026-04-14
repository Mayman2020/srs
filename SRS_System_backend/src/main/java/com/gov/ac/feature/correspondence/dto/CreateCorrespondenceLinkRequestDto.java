package com.gov.ac.feature.correspondence.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateCorrespondenceLinkRequestDto(
    @NotNull UUID linkedCorrespondenceId,
    String linkKind,
    String notes) {}
