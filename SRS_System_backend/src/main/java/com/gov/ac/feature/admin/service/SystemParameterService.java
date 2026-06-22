package com.gov.ac.feature.admin.service;

import com.gov.ac.feature.admin.repository.SystemParameterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SystemParameterService {

  public static final String WORKFLOW_DEFAULT_SLA_HOURS = "workflow.default_sla_hours";

  private final SystemParameterRepository systemParameterRepository;

  @Transactional(readOnly = true)
  public int getInt(String key, int fallback) {
    return systemParameterRepository
        .findById(key)
        .map(
            row -> {
              try {
                return Integer.parseInt(row.getParamValue().trim());
              } catch (NumberFormatException ex) {
                return fallback;
              }
            })
        .orElse(fallback);
  }

  @Transactional(readOnly = true)
  public String getDefaultSlaIsoDuration() {
    int hours = getInt(WORKFLOW_DEFAULT_SLA_HOURS, 72);
    if (hours <= 0) {
      hours = 72;
    }
    return "PT" + hours + "H";
  }
}
