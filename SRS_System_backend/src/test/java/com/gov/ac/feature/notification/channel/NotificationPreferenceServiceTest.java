package com.gov.ac.feature.notification.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gov.ac.feature.notification.channel.dto.NotificationPreferenceUpsertDto;
import com.gov.ac.feature.notification.channel.entity.NotificationPreferenceEntity;
import com.gov.ac.feature.notification.channel.repository.NotificationPreferenceRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {

  @Mock private NotificationPreferenceRepository preferenceRepository;

  @InjectMocks private NotificationPreferenceService preferenceService;

  @Test
  void missingRowMeansEnabled() {
    UUID user = UUID.randomUUID();
    when(preferenceRepository.findByUserIdAndEventTypeCodeAndChannelCodeAndDeletedAtIsNull(
            user, "OVERDUE", "IN_APP"))
        .thenReturn(Optional.empty());
    assertThat(preferenceService.isEnabled(user, "OVERDUE", "IN_APP")).isTrue();
  }

  @Test
  void explicitFalseDisables() {
    UUID user = UUID.randomUUID();
    NotificationPreferenceEntity row = new NotificationPreferenceEntity();
    row.setEnabled(false);
    when(preferenceRepository.findByUserIdAndEventTypeCodeAndChannelCodeAndDeletedAtIsNull(
            user, "OVERDUE", "EMAIL"))
        .thenReturn(Optional.of(row));
    assertThat(preferenceService.isEnabled(user, "OVERDUE", "EMAIL")).isFalse();
  }

  @Test
  void upsertOwnCreatesWhenMissing() {
    UUID user = UUID.randomUUID();
    when(preferenceRepository.findByUserIdAndEventTypeCodeAndChannelCodeAndDeletedAtIsNull(
            user, "X", "IN_APP"))
        .thenReturn(Optional.empty());
    preferenceService.upsertOwn(
        user, java.util.List.of(new NotificationPreferenceUpsertDto("X", "IN_APP", false)));
    verify(preferenceRepository).save(any(NotificationPreferenceEntity.class));
  }

  @Test
  void upsertOwnUpdatesWhenPresent() {
    UUID user = UUID.randomUUID();
    NotificationPreferenceEntity row = new NotificationPreferenceEntity();
    row.setEnabled(true);
    when(preferenceRepository.findByUserIdAndEventTypeCodeAndChannelCodeAndDeletedAtIsNull(
            user, "X", "IN_APP"))
        .thenReturn(Optional.of(row));
    preferenceService.upsertOwn(
        user, java.util.List.of(new NotificationPreferenceUpsertDto("X", "IN_APP", false)));
    verify(preferenceRepository).save(row);
  }
}