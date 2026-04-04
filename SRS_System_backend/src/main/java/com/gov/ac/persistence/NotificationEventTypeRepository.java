package com.gov.ac.persistence;

import com.gov.ac.domain.lookup.NotificationEventType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationEventTypeRepository extends JpaRepository<NotificationEventType, Long> {

  Optional<NotificationEventType> findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(String code);
}
