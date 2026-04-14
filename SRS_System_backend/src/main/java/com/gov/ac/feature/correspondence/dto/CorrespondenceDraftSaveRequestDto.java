package com.gov.ac.feature.correspondence.dto;

import jakarta.validation.constraints.Size;

/** When {@code bodyHtml} is blank after trim, the draft is cleared. */
public record CorrespondenceDraftSaveRequestDto(@Size(max = 500_000) String bodyHtml) {}
