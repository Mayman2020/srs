package com.gov.ac.feature.reports.controller;

import com.gov.ac.feature.dashboard.dto.DashboardBucketDto;
import com.gov.ac.feature.reports.dto.DepartmentSlaRowDto;
import com.gov.ac.feature.reports.dto.ReportMonthlyPointDto;
import com.gov.ac.feature.reports.dto.WorkflowSlaPointDto;
import com.gov.ac.feature.reports.service.ReportService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@PreAuthorize("@effectivePermission.has('REPORT_VIEW')")
public class ReportsController {

  private final ReportService reportService;

  @GetMapping("/status-distribution")
  public List<DashboardBucketDto> statusDistribution() {
    return reportService.statusDistribution();
  }

  @GetMapping("/priority-distribution")
  public List<DashboardBucketDto> priorityDistribution() {
    return reportService.priorityDistribution();
  }

  @GetMapping("/org-level-distribution")
  public List<DashboardBucketDto> orgLevelDistribution() {
    return reportService.orgLevelDistribution();
  }

  @GetMapping("/confidentiality-distribution")
  public List<DashboardBucketDto> confidentialityDistribution() {
    return reportService.confidentialityDistribution();
  }

  @GetMapping("/monthly-trend")
  public List<ReportMonthlyPointDto> monthlyTrend(
      @RequestParam(required = false) Instant from,
      @RequestParam(required = false) Instant to) {
    Instant toEx = to != null ? to : Instant.now().plus(1, ChronoUnit.DAYS);
    Instant fromIn =
        from != null ? from : toEx.minus(730, ChronoUnit.DAYS); // default ~24 months
    return reportService.monthlyTrend(fromIn, toEx);
  }

  @GetMapping("/department-sla-heatmap")
  public List<DepartmentSlaRowDto> departmentSla(
      @RequestParam(required = false) Instant now) {
    return reportService.departmentSlaHeatmap(now != null ? now : Instant.now());
  }

  /**
   * Routing-time SLA chart: monthly average end-to-end routing duration (seconds) for completed
   * workflows. Defaults to the last 12 months when {@code from}/{@code to} are missing.
   */
  @GetMapping("/workflow-sla-trend")
  public List<WorkflowSlaPointDto> workflowSlaTrend(
      @RequestParam(required = false) Instant from,
      @RequestParam(required = false) Instant to) {
    Instant toEx = to != null ? to : Instant.now().plus(1, ChronoUnit.DAYS);
    Instant fromIn = from != null ? from : toEx.minus(365, ChronoUnit.DAYS);
    return reportService.workflowSlaTrend(fromIn, toEx);
  }

  @GetMapping("/export/excel")
  @PreAuthorize("@effectivePermission.has('REPORT_EXPORT')")
  public ResponseEntity<byte[]> exportCorrespondencesExcel() {
    byte[] data = reportService.exportCorrespondencesExcel();
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"correspondences-export.xlsx\"")
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(data);
  }
}
