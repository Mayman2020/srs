package com.gov.ac.feature.reports.mapper;

import com.gov.ac.feature.reports.dto.DepartmentSlaRowDto;
import com.gov.ac.feature.reports.dto.ReportMonthlyPointDto;
import java.sql.Timestamp;
import java.time.YearMonth;
import java.time.ZoneOffset;

public final class ReportMapper {

  private ReportMapper() {}

  public static ReportMonthlyPointDto toMonthlyPoint(Object[] row) {
    Timestamp timestamp = (Timestamp) row[0];
    Number count = (Number) row[1];
    YearMonth yearMonth = YearMonth.from(timestamp.toInstant().atZone(ZoneOffset.UTC));
    return new ReportMonthlyPointDto(yearMonth.toString(), count.longValue());
  }

  public static DepartmentSlaRowDto toDepartmentSlaRow(Object[] row) {
    return new DepartmentSlaRowDto(
        ((Number) row[0]).longValue(),
        (String) row[1],
        (String) row[2],
        (String) row[3],
        ((Number) row[4]).longValue(),
        ((Number) row[5]).longValue());
  }
}
