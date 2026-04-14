package com.gov.ac.feature.communication.mapper;

import com.gov.ac.feature.communication.dto.CircularInboxRowDto;
import com.gov.ac.feature.communication.entity.CircularEntity;

public final class CircularMapper {

  private CircularMapper() {}

  public static CircularInboxRowDto toInboxRow(CircularEntity circular, boolean read) {
    return new CircularInboxRowDto(
        circular.getId(),
        circular.getTitle(),
        circular.getCreatedBy(),
        circular.getCreatedAt(),
        circular.isBroadcast(),
        read);
  }
}
