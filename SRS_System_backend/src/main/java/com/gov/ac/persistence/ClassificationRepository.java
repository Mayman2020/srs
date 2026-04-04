package com.gov.ac.persistence;

import com.gov.ac.domain.org.Classification;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassificationRepository extends JpaRepository<Classification, Long> {

  Optional<Classification> findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(String code);
}
