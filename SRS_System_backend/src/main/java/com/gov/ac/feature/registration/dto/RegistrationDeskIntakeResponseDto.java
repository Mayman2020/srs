package com.gov.ac.feature.registration.dto;

import java.util.UUID;

public record RegistrationDeskIntakeResponseDto(
    UUID id, String referenceNumber, String barcodeValue, String deskMode) {}
