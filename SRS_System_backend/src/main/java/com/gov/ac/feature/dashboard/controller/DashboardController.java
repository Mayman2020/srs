package com.gov.ac.feature.dashboard.controller;

import com.gov.ac.feature.dashboard.dto.DashboardResponseDto;
import com.gov.ac.feature.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Frontend: {@code features/dashboard} */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@PreAuthorize("@effectivePermission.has('DASHBOARD_VIEW')")
public class DashboardController {

  private final DashboardService dashboardService;

  @GetMapping
  public DashboardResponseDto dashboard() {
    return dashboardService.getDashboard();
  }
}
