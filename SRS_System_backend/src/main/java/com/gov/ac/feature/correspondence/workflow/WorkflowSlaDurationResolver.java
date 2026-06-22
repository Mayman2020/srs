package com.gov.ac.feature.correspondence.workflow;

import com.gov.ac.feature.admin.service.SystemParameterService;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.sla.entity.SlaPolicyEntity;
import com.gov.ac.feature.sla.service.SlaPolicyResolverService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkflowSlaDurationResolver {

  private final SlaPolicyResolverService slaPolicyResolverService;
  private final SystemParameterService systemParameterService;

  public String resolveSlaIso(CorrespondenceEntity correspondence, String levelCode) {
    return slaPolicyResolverService
        .resolveFor(correspondence, levelCode, null)
        .map(SlaPolicyEntity::getTargetHours)
        .filter(h -> h != null && h > 0)
        .map(h -> "PT" + h + "H")
        .orElseGet(systemParameterService::getDefaultSlaIsoDuration);
  }
}
