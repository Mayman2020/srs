package com.gov.ac.feature.admin.controller;

import com.gov.ac.feature.admin.dto.ReportSystemIssueRequestDto;
import com.gov.ac.feature.admin.service.SystemIssueAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system-issues")
@RequiredArgsConstructor
public class SystemIssueReportController {

  private final SystemIssueAdminService systemIssueAdminService;

  @PostMapping("/report")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void report(@Valid @RequestBody ReportSystemIssueRequestDto body) {
    systemIssueAdminService.reportClientIssue(body);
  }
}
