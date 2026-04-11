package com.gov.ac.correspondence.dto;

public record UpsertAttachmentIndexEntryRequest(
    Integer pageFrom, Integer pageTo, String subjectText, int sortOrder) {}
