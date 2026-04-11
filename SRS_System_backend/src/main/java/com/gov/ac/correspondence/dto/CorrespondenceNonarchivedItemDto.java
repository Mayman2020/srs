package com.gov.ac.correspondence.dto;

public record CorrespondenceNonarchivedItemDto(
    Long id, String itemType, String descriptionText, int quantity, int sortOrder) {}
