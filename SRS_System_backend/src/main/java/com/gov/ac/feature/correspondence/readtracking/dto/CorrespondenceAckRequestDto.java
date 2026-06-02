package com.gov.ac.feature.correspondence.readtracking.dto;

import jakarta.validation.constraints.Size;

/**
 * Body of POST {@code /correspondence/{id}/ack}. {@code comment} is optional and capped to keep
 * the audit detail JSON bounded.
 */
public record CorrespondenceAckRequestDto(@Size(max = 1000) String comment) {}
