package com.gov.ac.feature.notification.channel;

import com.gov.ac.feature.notification.channel.dto.NotificationPreferenceRowDto;
import com.gov.ac.feature.notification.channel.dto.NotificationPreferenceUpsertDto;
import com.gov.ac.feature.notification.channel.entity.NotificationPreferenceEntity;
import com.gov.ac.feature.notification.channel.repository.NotificationPreferenceRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

  private final NotificationPreferenceRepository preferenceRepository;

  /** Explicit opt-out only: missing row means enabled. */
  public boolean isEnabled(UUID userId, String eventTypeCode, String channelCode) {
    return preferenceRepository
        .findByUserIdAndEventTypeCodeAndChannelCodeAndDeletedAtIsNull(userId, eventTypeCode, channelCode)
        .map(p -> Boolean.TRUE.equals(p.getEnabled()))
        .orElse(true);
  }

  @Transactional(readOnly = true)
  public List<NotificationPreferenceRowDto> listForUser(UUID userId) {
    return preferenceRepository.findAllByUserIdAndDeletedAtIsNull(userId).stream()
        .map(
            e ->
                new NotificationPreferenceRowDto(
                    e.getId(), e.getEventTypeCode(), e.getChannelCode(), Boolean.TRUE.equals(e.getEnabled())))
        .toList();
  }

  @Transactional
  public void upsertOwn(UUID actorUserId, List<NotificationPreferenceUpsertDto> rows) {
    for (NotificationPreferenceUpsertDto row : rows) {
      var existing =
          preferenceRepository.findByUserIdAndEventTypeCodeAndChannelCodeAndDeletedAtIsNull(
              actorUserId, row.eventTypeCode(), row.channelCode());
      if (existing.isPresent()) {
        NotificationPreferenceEntity e = existing.get();
        e.setEnabled(row.enabled());
        preferenceRepository.save(e);
      } else {
        NotificationPreferenceEntity e = new NotificationPreferenceEntity();
        e.setUserId(actorUserId);
        e.setEventTypeCode(row.eventTypeCode());
        e.setChannelCode(row.channelCode());
        e.setEnabled(row.enabled());
        preferenceRepository.save(e);
      }
    }
  }
}
