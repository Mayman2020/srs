package com.gov.ac.feature.notification.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gov.ac.feature.notification.channel.entity.NotificationOutboxEntity;
import com.gov.ac.feature.notification.channel.repository.NotificationChannelTargetRepository;
import com.gov.ac.feature.notification.channel.repository.NotificationOutboxRepository;
import com.gov.ac.feature.users.repository.AppUserRepository;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NotificationOutboxServiceTest {

  @Test
  void occurrenceKeyKeepsSeparateWorkflowAndCommentEventsFromBeingDeduplicated() {
    NotificationOutboxRepository repository = mock(NotificationOutboxRepository.class);
    NotificationPreferenceService preferences = mock(NotificationPreferenceService.class);
    when(preferences.isEnabled(any(), any(), any())).thenReturn(true);
    NotificationOutboxService service =
        new NotificationOutboxService(
            repository,
            preferences,
            mock(NotificationChannelTargetRepository.class),
            new NotificationTeamsProperties(""),
            mock(AppUserRepository.class),
            new ObjectMapper());

    UUID recipient = UUID.randomUUID();
    UUID correspondence = UUID.randomUUID();
    service.enqueueInApp(
        recipient, "ASSIGNED", correspondence, "workflow.completed", Map.of(), "task-1");
    service.enqueueInApp(
        recipient, "ASSIGNED", correspondence, "workflow.completed", Map.of(), "task-2");

    ArgumentCaptor<NotificationOutboxEntity> rows =
        ArgumentCaptor.forClass(NotificationOutboxEntity.class);
    verify(repository, org.mockito.Mockito.times(2)).save(rows.capture());
    assertThat(rows.getAllValues())
        .extracting(NotificationOutboxEntity::getIdempotencyKey)
        .containsExactly(
            "ASSIGNED:IN_APP:" + recipient + ":" + correspondence + ":workflow.completed:task-1",
            "ASSIGNED:IN_APP:" + recipient + ":" + correspondence + ":workflow.completed:task-2")
        .doesNotHaveDuplicates();
  }

  @Test
  void longIdempotencyKeysAreCompactedToFitTheDatabaseColumn() {
    NotificationOutboxRepository repository = mock(NotificationOutboxRepository.class);
    NotificationPreferenceService preferences = mock(NotificationPreferenceService.class);
    when(preferences.isEnabled(any(), any(), any())).thenReturn(true);
    NotificationOutboxService service =
        new NotificationOutboxService(
            repository,
            preferences,
            mock(NotificationChannelTargetRepository.class),
            new NotificationTeamsProperties(""),
            mock(AppUserRepository.class),
            new ObjectMapper());

    service.enqueueInApp(
        UUID.randomUUID(), "E".repeat(64), UUID.randomUUID(), "M".repeat(128), Map.of(), "task-1");

    ArgumentCaptor<NotificationOutboxEntity> row =
        ArgumentCaptor.forClass(NotificationOutboxEntity.class);
    verify(repository).save(row.capture());
    assertThat(row.getValue().getIdempotencyKey())
        .startsWith("sha256:")
        .hasSizeLessThanOrEqualTo(128);
  }
}
