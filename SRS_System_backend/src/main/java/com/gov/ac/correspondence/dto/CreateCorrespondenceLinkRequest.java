package com.gov.ac.correspondence.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateCorrespondenceLinkRequest(
    @NotNull UUID linkedCorrespondenceId,
    String linkKind,
    String notes) {}
