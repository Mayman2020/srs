package com.gov.ac.feature.reports.service;

import com.gov.ac.feature.dashboard.dto.DashboardBucketDto;
import com.gov.ac.feature.dashboard.mapper.DashboardMapper;
import com.gov.ac.feature.reports.dto.DepartmentSlaRowDto;
import com.gov.ac.feature.reports.dto.ReportMonthlyPointDto;
import com.gov.ac.feature.reports.mapper.ReportMapper;
import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageRequest;
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
    return rows.stream().map(ReportMapper::toMonthlyPoint).toList();
  }

  @Transactional(readOnly = true)
  public byte[] exportCorrespondencesExcel() {
    var page = correspondenceRepository.exportRows(PageRequest.of(0, 50_000));
    try (XSSFWorkbook wb = new XSSFWorkbook();
        ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      Sheet sh = wb.createSheet("Correspondences");
      Row h = sh.createRow(0);
      h.createCell(0).setCellValue("referenceNumber");
      h.createCell(1).setCellValue("subject");
      h.createCell(2).setCellValue("typeCode");
      h.createCell(3).setCellValue("statusCode");
      h.createCell(4).setCellValue("createdAt");
      h.createCell(5).setCellValue("updatedAt");
      int r = 1;
      for (Object[] row : page.getContent()) {
        Row xr = sh.createRow(r++);
        xr.createCell(0).setCellValue(row[0] != null ? row[0].toString() : "");
        xr.createCell(1).setCellValue(row[1] != null ? row[1].toString() : "");
        xr.createCell(2).setCellValue(row[2] != null ? row[2].toString() : "");
        xr.createCell(3).setCellValue(row[3] != null ? row[3].toString() : "");
        if (row[4] instanceof Instant instant) {
          xr.createCell(4).setCellValue(instant.toString());
        } else {
          xr.createCell(4).setCellValue("");
        }
        if (row[5] instanceof Instant instant2) {
          xr.createCell(5).setCellValue(instant2.toString());
        } else {
          xr.createCell(5).setCellValue("");
        }
      }
      for (int c = 0; c < 6; c++) {
        sh.autoSizeColumn(c);
      }
      wb.write(bos);
      return bos.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Transactional(readOnly = true)
  public List<DepartmentSlaRowDto> departmentSlaHeatmap(Instant now) {
    List<Object[]> rows = correspondenceRepository.departmentSlaHeatmap(now);
    return rows.stream().map(ReportMapper::toDepartmentSlaRow).toList();
  }
}
