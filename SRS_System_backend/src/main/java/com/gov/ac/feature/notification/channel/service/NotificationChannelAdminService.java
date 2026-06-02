package com.gov.ac.feature.notification.channel.service;

import com.gov.ac.common.api.BadRequestException;
import com.gov.ac.common.api.NotFoundException;
import com.gov.ac.feature.notification.channel.dto.NotificationChannelTargetAdminDto;
import com.gov.ac.feature.notification.channel.dto.NotificationChannelTargetCreateDto;
import com.gov.ac.feature.notification.channel.dto.NotificationChannelTargetUpdateDto;
import com.gov.ac.feature.notification.channel.entity.NotificationChannelTargetEntity;
import com.gov.ac.feature.notification.channel.repository.NotificationChannelTargetRepository;
import com.gov.ac.security.SecurityUtils;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationChannelAdminService {

  private final NotificationChannelTargetRepository targetRepository;

  @Transactional(readOnly = true)
  public List<NotificationChannelTargetAdminDto> list() {
    return targetRepository.findAll().stream()
        .filter(t -> t.getDeletedAt() == null)
        .map(NotificationChannelAdminService::toDto)
        .toList();
  }

  @Transactional
  public NotificationChannelTargetAdminDto create(NotificationChannelTargetCreateDto dto) {
    targetRepository
        .findByChannelCodeAndTargetCodeAndDeletedAtIsNull(dto.channelCode(), dto.targetCode())
        .ifPresent(
            x -> {
              throw new BadRequestException("target_code already exists for channel");
            });
    NotificationChannelTargetEntity e = new NotificationChannelTargetEntity();
    e.setChannelCode(dto.channelCode());
    e.setTargetCode(dto.targetCode());
    e.setTargetUrl(dto.targetUrl());
    e.setSigningSecretRef(dto.signingSecretRef());
    e.setEnabled(dto.enabled());
    e.setDescription(dto.description());
    return toDto(targetRepository.save(e));
  }

  @Transactional
  public NotificationChannelTargetAdminDto update(UUID id, NotificationChannelTargetUpdateDto dto) {
    NotificationChannelTargetEntity e =
        targetRepository.findById(id).orElseThrow(() -> new NotFoundException("target not found"));
    if (e.getDeletedAt() != null) {
      throw new NotFoundException("target not found");
    }
    if (dto.targetUrl() != null) {
      e.setTargetUrl(dto.targetUrl());
    }
    if (dto.signingSecretRef() != null) {
      e.setSigningSecretRef(dto.signingSecretRef());
    }
    if (dto.enabled() != null) {
      e.setEnabled(dto.enabled());
    }
    if (dto.description() != null) {
      e.setDescription(dto.description());
    }
    return toDto(targetRepository.save(e));
  }

  @Transactional
  public void delete(UUID id) {
    NotificationChannelTargetEntity e =
        targetRepository.findById(id).orElseThrow(() -> new NotFoundException("target not found"));
    if (e.getDeletedAt() != null) {
      return;
    }
    UUID actor = SecurityUtils.requireCurrentUserId();
    e.setDeletedAt(Instant.now());
    e.setDeletedBy(actor);
    targetRepository.save(e);
  }

  private static NotificationChannelTargetAdminDto toDto(NotificationChannelTargetEntity e) {
    return new NotificationChannelTargetAdminDto(
        e.getId(),
        e.getChannelCode(),
        e.getTargetCode(),
        e.getTargetUrl(),
        e.getSigningSecretRef(),
        Boolean.TRUE.equals(e.getEnabled()),
        e.getDescription());
  }
}
