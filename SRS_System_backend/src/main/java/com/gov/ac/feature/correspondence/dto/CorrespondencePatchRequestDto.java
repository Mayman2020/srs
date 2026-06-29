package com.gov.ac.feature.correspondence.dto;

import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

/** Partial update for mutable correspondence metadata (US-B04). */
public record CorrespondencePatchRequestDto(
    @Size(max = 500) String subject,
    @Size(max = 20000) String description,
    @Size(max = 500000) String bodyHtml,
    @Size(max = 64) String priorityCode,
    @Size(max = 64) String confidentialityCode,
    @Size(max = 64) String classificationCode,
    Long senderOrganizationId,
    Long recipientOrganizationId,
    @Size(max = 128) String externalReferenceNumber,
    LocalDate externalReferenceDate,
    Long ownerDepartmentId,
    Instant dueDate,
    @Size(max = 100) String barcodeValue,
    @Size(max = 500) String beneficiaryName,
    @Size(max = 500) String beneficiaryOrganization,
    @Size(max = 128) String beneficiaryIdentifier) {}
