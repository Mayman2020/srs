package com.gov.ac.feature.correspondence.dto;

import java.time.Instant;

public record CorrespondenceUserRecipientDto(
    long id,
    String recipientUserId,
    String recipientUsername,
    String recipientFullNameAr,
    String recipientFullNameEn,
    String recipientKindCode,
    Instant firstReadAt,
    Instant lastReadAt,
    int readCount,
    Instant acknowledgedAt) {}
