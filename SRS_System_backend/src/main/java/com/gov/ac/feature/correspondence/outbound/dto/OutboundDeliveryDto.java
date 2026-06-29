package com.gov.ac.feature.correspondence.outbound.dto;

import java.time.Instant;
import java.util.UUID;

public record OutboundDeliveryDto(
    long id,
    UUID correspondenceId,
    String correspondenceReferenceNumber,
    String correspondenceSubject,
    String channelCode,
    String statusCode,
    String recipientLabel,
    String proofReference,
    String notes,
    Instant sentAt,
    Instant deliveredAt,
    Instant updatedAt) {}
