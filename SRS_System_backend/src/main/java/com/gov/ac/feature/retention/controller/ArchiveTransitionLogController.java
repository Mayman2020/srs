package com.gov.ac.feature.retention.controller;

import com.gov.ac.feature.retention.dto.ArchiveTransitionLogDto;
import com.gov.ac.feature.retention.service.RetentionAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/retention/archive-log")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ArchiveTransitionLogController {

  private final RetentionAdminService retentionAdminService;

  @GetMapping
  @PreAuthorize("@effectivePermission.has('RETENTION_LOG_VIEW')")
  public Page<ArchiveTransitionLogDto> page(Pageable pageable) {
    return retentionAdminService.pageLog(pageable);
  }
}
