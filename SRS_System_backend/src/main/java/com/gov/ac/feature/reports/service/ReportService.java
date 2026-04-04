package com.gov.ac.feature.reports.service;

import com.gov.ac.feature.dashboard.dto.DashboardBucketDto;
import com.gov.ac.feature.dashboard.mapper.DashboardMapper;
import com.gov.ac.feature.reports.dto.DepartmentSlaRowDto;
import com.gov.ac.feature.reports.dto.ReportMonthlyPointDto;
import com.gov.ac.persistence.CorrespondenceRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

  private final CorrespondenceRepository correspondenceRepository;

  @Transactional(readOnly = true)
  public List<DashboardBucketDto> statusDistribution() {
    return DashboardMapper.mapBuckets(
        correspondenceRepository.aggregateActiveByCorrespondenceStatus());
  }

  @Transactional(readOnly = true)
  public List<DashboardBucketDto> priorityDistribution() {
    return DashboardMapper.mapBuckets(correspondenceRepository.aggregateActiveByPriority());
  }

  @Transactional(readOnly = true)
  public List<ReportMonthlyPointDto> monthlyTrend(Instant fromInclusive, Instant toExclusive) {
    List<Object[]> rows =
        correspondenceRepository.countCreatedByMonth(fromInclusive, toExclusive);
    return rows.stream()
        .map(
            r -> {
              Timestamp ts = (Timestamp) r[0];
              Number cnt = (Number) r[1];
              YearMonth ym = YearMonth.from(ts.toInstant().atZone(ZoneOffset.UTC));
              return new ReportMonthlyPointDto(ym.toString(), cnt.longValue());
            })
        .toList();
  }

  @Transactional(readOnly = true)
  public List<DepartmentSlaRowDto> departmentSlaHeatmap(Instant now) {
    List<Object[]> rows = correspondenceRepository.departmentSlaHeatmap(now);
    return rows.stream()
        .map(
            r ->
                new DepartmentSlaRowDto(
                    ((Number) r[0]).longValue(),
                    (String) r[1],
                    (String) r[2],
                    (String) r[3],
                    ((Number) r[4]).longValue(),
                    ((Number) r[5]).longValue()))
        .toList();
  }
}
