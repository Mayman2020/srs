package com.gov.ac.correspondence.dto;

import java.util.UUID;

public record CorrespondenceLinkListItemDto(
    Long id,
    UUID linkedCorrespondenceId,
    String linkedReferenceNumber,
    String linkedSubject,
    String linkKind,
    String notes) {}
