package com.gov.ac.feature.reports.service;

import com.gov.ac.feature.correspondence.repository.CorrespondenceRepository;
import com.gov.ac.feature.correspondence.security.DepartmentScopeResolver;
import com.gov.ac.feature.dashboard.dto.DashboardBucketDto;
import com.gov.ac.feature.dashboard.mapper.DashboardMapper;
import com.gov.ac.feature.lookups.entity.ConfidentialityEntity;
import com.gov.ac.feature.lookups.repository.ConfidentialityRepository;
import com.gov.ac.feature.reports.dto.DepartmentSlaRowDto;
import com.gov.ac.feature.reports.dto.ReportMonthlyPointDto;
import com.gov.ac.feature.reports.dto.WorkflowSlaPointDto;
import com.gov.ac.feature.reports.mapper.ReportMapper;
import com.gov.ac.feature.users.repository.AppUserRepository;
import com.gov.ac.feature.workflow.execution.repository.WorkflowInstanceRepository;
import com.gov.ac.security.SecurityUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
  private final DepartmentScopeResolver departmentScopeResolver;
  private final AppUserRepository appUserRepository;
  private final ConfidentialityRepository confidentialityRepository;
  private final WorkflowInstanceRepository workflowInstanceRepository;

  @Transactional(readOnly = true)
  public List<DashboardBucketDto> statusDistribution() {
    Long deptScope = currentDeptScope();
    return DashboardMapper.mapBuckets(
        correspondenceRepository.aggregateActiveByCorrespondenceStatusScoped(deptScope));
  }

  @Transactional(readOnly = true)
  public List<DashboardBucketDto> priorityDistribution() {
    Long deptScope = currentDeptScope();
    return DashboardMapper.mapBuckets(
        correspondenceRepository.aggregateActiveByPriorityScoped(deptScope));
  }

  @Transactional(readOnly = true)
  public List<DashboardBucketDto> orgLevelDistribution() {
    Long deptScope = currentDeptScope();
    return DashboardMapper.mapBuckets(
        correspondenceRepository.aggregateActiveByOrgLevelScoped(deptScope));
  }

  @Transactional(readOnly = true)
  public List<DashboardBucketDto> confidentialityDistribution() {
    Long deptScope = currentDeptScope();
    return DashboardMapper.mapBuckets(
        correspondenceRepository.aggregateActiveByConfidentialityScoped(deptScope));
  }

  private Long currentDeptScope() {
    UUID userId = SecurityUtils.requireCurrentUserId();
    return departmentScopeResolver.resolveDepartmentScope(userId);
  }

  @Transactional(readOnly = true)
  public List<ReportMonthlyPointDto> monthlyTrend(Instant fromInclusive, Instant toExclusive) {
    List<Object[]> rows =
        correspondenceRepository.countCreatedByMonth(fromInclusive, toExclusive);
    return rows.stream().map(ReportMapper::toMonthlyPoint).toList();
  }

  /**
   * Excel export honoring department scope and the caller's clearance:
   *
   * <ul>
   *   <li>Privileged users get the global dataset.
   *   <li>Other users only see their own department's correspondences.
   *   <li>Restricted-confidentiality rows are filtered out when the caller's clearance is not
   *       sufficient (lower {@code sort_order} = more restrictive).
   * </ul>
   */
  @Transactional(readOnly = true)
  public byte[] exportCorrespondencesExcel() {
    UUID userId = SecurityUtils.requireCurrentUserId();
    Long deptScope = departmentScopeResolver.resolveDepartmentScope(userId);
    Integer viewerSortOrder = resolveViewerClearanceSortOrder(userId);

    var page =
        correspondenceRepository.exportRowsScoped(
            deptScope, viewerSortOrder, PageRequest.of(0, 50_000));
    try (XSSFWorkbook wb = new XSSFWorkbook();
        ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      Sheet sh = wb.createSheet("Correspondences");
      Row h = sh.createRow(0);
      h.createCell(0).setCellValue("referenceNumber");
      h.createCell(1).setCellValue("subject");
      h.createCell(2).setCellValue("typeCode");
      h.createCell(3).setCellValue("statusCode");
      h.createCell(4).setCellValue("confidentialityCode");
      h.createCell(5).setCellValue("levelCode");
      h.createCell(6).setCellValue("createdAt");
      h.createCell(7).setCellValue("updatedAt");
      int r = 1;
      for (Object[] row : page.getContent()) {
        Row xr = sh.createRow(r++);
        xr.createCell(0).setCellValue(row[0] != null ? row[0].toString() : "");
        xr.createCell(1).setCellValue(row[1] != null ? row[1].toString() : "");
        xr.createCell(2).setCellValue(row[2] != null ? row[2].toString() : "");
        xr.createCell(3).setCellValue(row[3] != null ? row[3].toString() : "");
        xr.createCell(4).setCellValue(row[4] != null ? row[4].toString() : "");
        xr.createCell(5).setCellValue(row[5] != null ? row[5].toString() : "");
        if (row[6] instanceof java.sql.Timestamp ts) {
          xr.createCell(6).setCellValue(ts.toInstant().toString());
        } else if (row[6] instanceof Instant instant) {
          xr.createCell(6).setCellValue(instant.toString());
        } else {
          xr.createCell(6).setCellValue(row[6] != null ? row[6].toString() : "");
        }
        if (row[7] instanceof java.sql.Timestamp ts2) {
          xr.createCell(7).setCellValue(ts2.toInstant().toString());
        } else if (row[7] instanceof Instant instant2) {
          xr.createCell(7).setCellValue(instant2.toString());
        } else {
          xr.createCell(7).setCellValue(row[7] != null ? row[7].toString() : "");
        }
      }
      for (int c = 0; c < 8; c++) {
        sh.autoSizeColumn(c);
      }
      wb.write(bos);
      return bos.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private Integer resolveViewerClearanceSortOrder(UUID userId) {
    return appUserRepository
        .findByIdAndDeletedAtIsNull(userId)
        .map(u -> u.getSecurityClearanceId())
        .flatMap(id -> confidentialityRepository.findByIdAndDeletedAtIsNull(id))
        .map(ConfidentialityEntity::getSortOrder)
        .orElse(null);
  }

  @Transactional(readOnly = true)
  public List<DepartmentSlaRowDto> departmentSlaHeatmap(Instant now) {
    List<Object[]> rows = correspondenceRepository.departmentSlaHeatmap(now);
    return rows.stream().map(ReportMapper::toDepartmentSlaRow).toList();
  }

  /**
   * Monthly average routing time (seconds) chart. Returns one point per month bucket within
   * [{@code fromInclusive}, {@code toExclusive}), scoped to the caller's department when the
   * caller is non-privileged.
   */
  @Transactional(readOnly = true)
  public List<WorkflowSlaPointDto> workflowSlaTrend(Instant fromInclusive, Instant toExclusive) {
    Long deptScope = currentDeptScope();
    List<Object[]> rows =
        workflowInstanceRepository.averageRoutingSecondsByMonth(
            fromInclusive, toExclusive, deptScope);
    return rows.stream().map(ReportService::toWorkflowSlaPoint).toList();
  }

  private static WorkflowSlaPointDto toWorkflowSlaPoint(Object[] row) {
    Instant bucket;
    Object raw = row[0];
    if (raw instanceof java.sql.Timestamp ts) {
      bucket = ts.toInstant();
    } else if (raw instanceof Instant i) {
      bucket = i;
    } else {
      bucket = null;
    }
    long avgSeconds = row[1] instanceof Number n ? n.longValue() : 0L;
    long completed = row[2] instanceof Number c ? c.longValue() : 0L;
    return new WorkflowSlaPointDto(bucket, avgSeconds, completed);
  }
}
