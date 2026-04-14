package com.gov.ac.feature.correspondence.dto;

public record AttachmentIndexEntryDto(
    Long id, Integer pageFrom, Integer pageTo, String subjectText, int sortOrder) {}
