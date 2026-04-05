package com.gov.ac.correspondence.dto;

import jakarta.validation.constraints.Size;

/** When {@code bodyHtml} is blank after trim, the draft is cleared. */
public record CorrespondenceDraftSaveRequest(@Size(max = 500_000) String bodyHtml) {}
