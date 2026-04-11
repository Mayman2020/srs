package com.gov.ac.feature.profile.controller;

import com.gov.ac.feature.profile.dto.UserCapabilitiesDto;
import com.gov.ac.feature.profile.service.UserCapabilitiesService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeCapabilitiesController {

  private final UserCapabilitiesService userCapabilitiesService;

  @GetMapping("/capabilities")
  public UserCapabilitiesDto capabilities() {
    return userCapabilitiesService.loadForCurrentUser();
  }
}
