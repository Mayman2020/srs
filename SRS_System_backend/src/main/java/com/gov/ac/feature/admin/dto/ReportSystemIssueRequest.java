package com.gov.ac.feature.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReportSystemIssueRequest(
    @NotBlank @Size(max = 32) String severity,
    @NotBlank @Size(max = 2000) String message,
    @Size(max = 12000) String detail,
    @Size(max = 2000) String pageUrl,
    Integer httpStatus) {}
