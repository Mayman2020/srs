package com.gov.ac.feature.correspondence.service;

import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.common.api.BadRequestException;

public final class CorrespondenceMutationGuards {

  private CorrespondenceMutationGuards() {}

  public static void assertCorrespondenceMutable(CorrespondenceEntity c) {
    if (c.getCorrespondenceStatus() == null) {
      return;
    }
    if (Boolean.TRUE.equals(c.getCorrespondenceStatus().getTerminal())) {
      throw new BadRequestException("CorrespondenceEntity cannot be modified in the current status");
    }
  }
}
