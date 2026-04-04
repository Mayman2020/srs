package com.gov.ac.web.dto;

import java.time.Instant;
import java.util.UUID;

public record CorrespondenceListDto(
    UUID id,
    String referenceNumber,
    String subject,
    String typeCode,
    String statusCode,
    String priorityCode,
    Instant createdAt) {}
