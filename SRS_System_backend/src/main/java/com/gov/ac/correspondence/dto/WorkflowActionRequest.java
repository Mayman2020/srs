package com.gov.ac.correspondence.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * @param action {@code APPROVE}, {@code REJECT}, or {@code RETURN}. When omitted, {@code APPROVE} is
 *     used (backward compatible).
 */
public record WorkflowActionRequest(
    @Pattern(regexp = "APPROVE|REJECT|RETURN", flags = Pattern.Flag.CASE_INSENSITIVE)
        String action,
    @Size(max = 20000) String comment) {}
