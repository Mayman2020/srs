package com.gov.ac.feature.dashboard.service;

import com.gov.ac.feature.dashboard.dto.DashboardBucketDto;
import com.gov.ac.feature.dashboard.dto.DashboardResponseDto;
import com.gov.ac.feature.dashboard.mapper.DashboardMapper;
import com.gov.ac.persistence.CorrespondenceRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

  private final CorrespondenceRepository correspondenceRepository;

  @Transactional(readOnly = true)
  public DashboardResponseDto getDashboard() {
    Instant now = Instant.now();
    long total = correspondenceRepository.countActive();
    List<DashboardBucketDto> byStatus =
        DashboardMapper.mapBuckets(correspondenceRepository.aggregateActiveByCorrespondenceStatus());
    List<DashboardBucketDto> byPriority =
        DashboardMapper.mapBuckets(correspondenceRepository.aggregateActiveByPriority());
    long overdue = correspondenceRepository.countOverdueOpen(now);
    return new DashboardResponseDto(total, byStatus, byPriority, overdue);
  }
}
