package com.gov.ac.feature.correspondence.outbound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record UpsertOutboundDeliveryRequestDto(
    @NotNull UUID correspondenceId,
    @NotBlank @Size(max = 32) String channelCode,
    @NotBlank @Size(max = 32) String statusCode,
    @Size(max = 500) String recipientLabel,
    @Size(max = 256) String proofReference,
    @Size(max = 4000) String notes,
    Instant sentAt,
    Instant deliveredAt) {}
