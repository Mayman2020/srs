package com.gov.ac.correspondence.dto;

public record AttachmentIndexEntryDto(
    Long id, Integer pageFrom, Integer pageTo, String subjectText, int sortOrder) {}
