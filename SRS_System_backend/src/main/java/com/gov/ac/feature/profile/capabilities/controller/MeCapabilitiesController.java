package com.gov.ac.feature.profile.capabilities.controller;

import com.gov.ac.feature.profile.capabilities.dto.ShellNavItemDto;
import com.gov.ac.feature.profile.capabilities.dto.UserCapabilitiesDto;
import com.gov.ac.feature.profile.capabilities.service.ShellNavigationService;
import com.gov.ac.feature.profile.capabilities.service.UserCapabilitiesService;
import com.gov.ac.security.SecurityUtils;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MeCapabilitiesController {

  private final UserCapabilitiesService userCapabilitiesService;
  private final ShellNavigationService shellNavigationService;

  @GetMapping("/api/v1/me/capabilities")
  public UserCapabilitiesDto capabilities() {
    return userCapabilitiesService.loadForCurrentUser();
  }

  /** Sidebar navigation from {@code ui_screen} filtered by effective permissions. */
  @GetMapping("/api/v1/profile/me/navigation")
  public List<ShellNavItemDto> navigation() {
    return shellNavigationService.navigationForUser(SecurityUtils.requireCurrentUserId());
  }
}
