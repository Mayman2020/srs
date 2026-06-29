package com.gov.ac.feature.registration.dto;

import java.time.Instant;
import java.util.UUID;

public record RegistrationDeskRowDto(
    UUID id,
    String referenceNumber,
    String barcodeValue,
    String subject,
    String correspondenceTypeCode,
    String deskMode,
    Instant createdAt) {}
