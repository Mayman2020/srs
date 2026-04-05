package com.gov.ac.feature.admin.dto;

import jakarta.validation.constraints.Size;

public record ResolveSystemIssueRequest(@Size(max = 2000) String resolutionNote) {}
