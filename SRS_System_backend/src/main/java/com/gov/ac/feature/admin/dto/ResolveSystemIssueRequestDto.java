package com.gov.ac.feature.admin.dto;

import jakarta.validation.constraints.Size;

public record ResolveSystemIssueRequestDto(@Size(max = 2000) String resolutionNote) {}
