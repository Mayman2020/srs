package com.gov.ac.feature.profile.dto;

import java.util.List;

public record UserCapabilitiesDto(
    List<String> roles, List<String> permissions, List<CapabilityScreenDto> screens) {}
