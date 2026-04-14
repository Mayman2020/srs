package com.gov.ac.feature.correspondence.dto;

public record CorrespondenceNonarchivedItemDto(
    Long id, String itemType, String descriptionText, int quantity, int sortOrder) {}
