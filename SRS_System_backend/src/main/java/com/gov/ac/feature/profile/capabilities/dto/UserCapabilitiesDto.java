package com.gov.ac.feature.profile.capabilities.dto;

import java.util.List;

public record UserCapabilitiesDto(
    List<String> roles, List<String> permissions, List<CapabilityScreenDto> screens) {}
