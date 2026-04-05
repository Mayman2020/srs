package com.gov.ac.correspondence.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record WorkflowDelegateRequest(@NotNull UUID delegateeUserId) {}
