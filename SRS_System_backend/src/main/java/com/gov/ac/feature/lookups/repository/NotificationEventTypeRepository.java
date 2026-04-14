package com.gov.ac.feature.lookups.repository;

import com.gov.ac.feature.lookups.entity.NotificationEventTypeEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationEventTypeRepository extends JpaRepository<NotificationEventTypeEntity, Long> {

  Optional<NotificationEventTypeEntity> findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(String code);
}
