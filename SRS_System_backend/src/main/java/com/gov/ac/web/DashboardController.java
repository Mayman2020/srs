package com.gov.ac.web;

import com.gov.ac.persistence.CorrespondenceRepository;
import com.gov.ac.web.dto.CodeCountDto;
import com.gov.ac.web.dto.DashboardChartsDto;
import com.gov.ac.web.dto.DashboardSummaryDto;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

  private final CorrespondenceRepository correspondenceRepository;

  @GetMapping("/summary")
  public DashboardSummaryDto summary() {
    long total = correspondenceRepository.countActive();
    long inbound = correspondenceRepository.countActiveByTypeCode("INBOUND");
    long outbound = correspondenceRepository.countActiveByTypeCode("OUTBOUND");
    long inProgress = correspondenceRepository.countActiveByStatusCode("IN_PROGRESS");
    long completed = correspondenceRepository.countActiveByStatusCode("COMPLETED");
    return new DashboardSummaryDto(total, inbound, outbound, inProgress, completed);
  }

  /** Counts grouped by lookup `code` for charts (labels from `/api/v1/lookups`). */
  @GetMapping("/charts")
  public DashboardChartsDto charts() {
    return new DashboardChartsDto(
        toCodeCounts(correspondenceRepository.countGroupedByCorrespondenceStatus()),
        toCodeCounts(correspondenceRepository.countGroupedByCorrespondenceType()),
        toCodeCounts(correspondenceRepository.countGroupedByPriority()));
  }

  private static List<CodeCountDto> toCodeCounts(List<Object[]> rows) {
    List<CodeCountDto> out = new ArrayList<>(rows.size());
    for (Object[] r : rows) {
      out.add(new CodeCountDto((String) r[0], ((Number) r[1]).longValue()));
    }
    return out;
  }
}
