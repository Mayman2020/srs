package com.gov.ac.correspondence.service;

import com.gov.ac.domain.correspondence.Correspondence;
import com.gov.ac.common.api.BadRequestException;

public final class CorrespondenceMutationGuards {

  private CorrespondenceMutationGuards() {}

  public static void assertCorrespondenceMutable(Correspondence c) {
    if (c.getCorrespondenceStatus() == null || c.getCorrespondenceStatus().getCode() == null) {
      return;
    }
    String code = c.getCorrespondenceStatus().getCode().trim().toUpperCase();
    if ("CANCELLED".equals(code)) {
      throw new BadRequestException("Correspondence is cancelled");
    }
  }
}
