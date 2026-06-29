package com.gov.ac.feature.correspondence.dto;

import java.time.Instant;

public record CorrespondenceRecipientDto(
    long id,
    long departmentId,
    String departmentCode,
    String departmentNameAr,
    String departmentNameEn,
    Instant firstReadAt,
    Instant lastReadAt,
    int readCount) {}
