package com.gov.ac.feature.correspondence.dto;

public record UpsertAttachmentIndexEntryRequestDto(
    Integer pageFrom, Integer pageTo, String subjectText, int sortOrder) {}
