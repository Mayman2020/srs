package com.gov.ac.feature.notification.channel.repository;

import com.gov.ac.feature.notification.channel.entity.NotificationOutboxEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationOutboxRepositoryImpl implements NotificationOutboxRepositoryCustom {

  @PersistenceContext private EntityManager entityManager;

  @SuppressWarnings("unchecked")
  @Override
  public List<NotificationOutboxEntity> findDueBatchForUpdateSkipLocked(Instant now, int limit) {
    return entityManager
        .createNativeQuery(
            """
            select o.* from srs_system.notification_outbox o
            where o.status = 'PENDING' and o.next_attempt_at <= :now
            order by o.next_attempt_at asc
            limit :lim
            for update skip locked
            """,
            NotificationOutboxEntity.class)
        .setParameter("now", now)
        .setParameter("lim", limit)
        .getResultList();
  }
}
