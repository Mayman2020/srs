package com.gov.ac.feature.dashboard.service;

import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import com.gov.ac.feature.correspondence.security.DepartmentScopeResolver;
import com.gov.ac.feature.dashboard.KpiSegmentCodes;
import com.gov.ac.feature.dashboard.dto.DashboardBucketDto;
import com.gov.ac.feature.dashboard.dto.DashboardResponseDto;
import com.gov.ac.feature.dashboard.mapper.DashboardMapper;
import com.gov.ac.security.SecurityUtils;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

  private final CorrespondenceRepository correspondenceRepository;
  private final DepartmentScopeResolver departmentScopeResolver;

  @Transactional(readOnly = true)
  public DashboardResponseDto getDashboard() {
    UUID userId = SecurityUtils.requireCurrentUserId();
    Long deptScope = departmentScopeResolver.resolveDepartmentScope(userId);

    Instant now = Instant.now();
    long total = correspondenceRepository.countActiveScoped(deptScope);
    List<DashboardBucketDto> byStatus =
        DashboardMapper.mapBuckets(
            correspondenceRepository.aggregateActiveByCorrespondenceStatusScoped(deptScope));
    List<DashboardBucketDto> byPriority =
        DashboardMapper.mapBuckets(
            correspondenceRepository.aggregateActiveByPriorityScoped(deptScope));
    List<DashboardBucketDto> byLevel =
        DashboardMapper.mapBuckets(
            correspondenceRepository.aggregateActiveByOrgLevelScoped(deptScope));
    List<DashboardBucketDto> byConfidentiality =
        DashboardMapper.mapBuckets(
            correspondenceRepository.aggregateActiveByConfidentialityScoped(deptScope));
    long overdue = correspondenceRepository.countOverdueOpenScoped(now, deptScope);
    long kpiDone =
        correspondenceRepository.countActiveByKpiSegmentScoped(KpiSegmentCodes.SLA_DONE, deptScope);
    long kpiPipe =
        correspondenceRepository.countActiveByKpiSegmentScoped(KpiSegmentCodes.PIPELINE, deptScope);
    long kpiInbox =
        correspondenceRepository.countActiveByKpiSegmentScoped(KpiSegmentCodes.INBOX, deptScope);
    long kpiOb = correspondenceRepository.countActiveOutboundHighlightedScoped(deptScope);
    return new DashboardResponseDto(
        total,
        byStatus,
        byPriority,
        byLevel,
        byConfidentiality,
        overdue,
        kpiDone,
        kpiPipe,
        kpiInbox,
        kpiOb);
  }
}
