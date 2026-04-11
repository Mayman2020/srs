package com.gov.ac.correspondence.service;

import com.gov.ac.domain.correspondence.Correspondence;
import com.gov.ac.common.api.BadRequestException;

public final class CorrespondenceMutationGuards {

  private CorrespondenceMutationGuards() {}

  public static void assertCorrespondenceMutable(Correspondence c) {
    if (c.getCorrespondenceStatus() == null) {
      return;
    }
    if (Boolean.TRUE.equals(c.getCorrespondenceStatus().getTerminal())) {
      throw new BadRequestException("Correspondence cannot be modified in the current status");
    }
  }
}
