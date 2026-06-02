package com.gov.ac.feature.notification.channel.repository;

import com.gov.ac.feature.notification.channel.entity.NotificationOutboxEntity;
import java.time.Instant;
import java.util.List;

public interface NotificationOutboxRepositoryCustom {

  List<NotificationOutboxEntity> findDueBatchForUpdateSkipLocked(Instant now, int limit);
}
