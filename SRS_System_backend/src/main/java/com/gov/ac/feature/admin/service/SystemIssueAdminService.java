package com.gov.ac.feature.admin.service;

import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.domain.admin.SystemIssue;
import com.gov.ac.domain.user.AppUser;
import com.gov.ac.feature.admin.dto.ReportSystemIssueRequest;
import com.gov.ac.feature.admin.dto.ResolveSystemIssueRequest;
import com.gov.ac.feature.admin.dto.SystemIssueDto;
import com.gov.ac.persistence.AppUserRepository;
import com.gov.ac.persistence.SystemIssueRepository;
import com.gov.ac.security.SecurityUtils;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SystemIssueAdminService {

  private final SystemIssueRepository systemIssueRepository;
  private final AppUserRepository appUserRepository;

  @Transactional
  public void reportClientIssue(ReportSystemIssueRequest req) {
    SystemIssue issue = new SystemIssue();
    issue.setSource("CLIENT");
    issue.setSeverity(normalizeSeverity(req.severity()));
    issue.setMessage(req.message().trim());
    issue.setDetail(trimToNull(req.detail()));
    issue.setPageUrl(trimToNull(req.pageUrl()));
    issue.setHttpStatus(req.httpStatus());
    UUID userId = tryCurrentUserId();
    if (userId != null) {
      appUserRepository.findById(userId).ifPresent(issue::setUser);
    }
    systemIssueRepository.save(issue);
  }

  @Transactional(readOnly = true)
  public List<SystemIssueDto> listRecent() {
    return systemIssueRepository.findTop200ByOrderByCreatedAtDesc().stream()
        .map(SystemIssueAdminService::toDto)
        .toList();
  }

  @Transactional
  public SystemIssueDto resolve(Long id, ResolveSystemIssueRequest req) {
    SystemIssue issue =
        systemIssueRepository.findById(id).orElseThrow(() -> new NotFoundException("Issue not found"));
    if (issue.getResolvedAt() != null) {
      return toDto(issue);
    }
    UUID actor = SecurityUtils.requireCurrentUserId();
    issue.setResolvedAt(Instant.now());
    issue.setResolvedBy(actor);
    issue.setResolutionNote(trimToNull(req.resolutionNote()));
    return toDto(systemIssueRepository.save(issue));
  }

  private static UUID tryCurrentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      return null;
    }
    Object p = auth.getPrincipal();
    if (p instanceof UUID u) {
      return u;
    }
    return null;
  }

  private static SystemIssueDto toDto(SystemIssue i) {
    AppUser u = i.getUser();
    return new SystemIssueDto(
        i.getId(),
        i.getSource(),
        i.getSeverity(),
        i.getMessage(),
        i.getDetail(),
        i.getPageUrl(),
        u != null ? u.getId() : null,
        i.getHttpStatus(),
        i.getCreatedAt(),
        i.getResolvedAt(),
        i.getResolvedBy(),
        i.getResolutionNote());
  }

  private static String normalizeSeverity(String s) {
    String t = s == null ? "ERROR" : s.trim().toUpperCase();
    if (!t.equals("ERROR") && !t.equals("WARN") && !t.equals("INFO")) {
      return "ERROR";
    }
    return t;
  }

  private static String trimToNull(String s) {
    if (s == null) {
      return null;
    }
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }
}
