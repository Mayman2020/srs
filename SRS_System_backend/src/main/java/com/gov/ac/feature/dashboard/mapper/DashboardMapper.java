package com.gov.ac.feature.dashboard.mapper;

import com.gov.ac.feature.dashboard.dto.DashboardBucketDto;
import java.util.List;

/** Maps JDBC aggregate rows to dashboard DTOs (same shape as {@code DashboardService} had inline). */
public final class DashboardMapper {

  private DashboardMapper() {}

  public static List<DashboardBucketDto> mapBuckets(List<Object[]> rows) {
    return rows.stream().map(DashboardMapper::toBucket).toList();
  }

  private static DashboardBucketDto toBucket(Object[] r) {
    return new DashboardBucketDto(
        ((Number) r[0]).longValue(),
        (String) r[1],
        (String) r[2],
        (String) r[3],
        ((Number) r[4]).intValue(),
        ((Number) r[5]).longValue());
  }
}
