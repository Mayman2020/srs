package com.gov.ac.feature.lookups.repository;

import com.gov.ac.feature.lookups.entity.AttachmentContentTypeEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentContentTypeRepository extends JpaRepository<AttachmentContentTypeEntity, Long> {

  Optional<AttachmentContentTypeEntity> findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(String code);
}
