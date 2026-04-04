package com.gov.ac.persistence;

import com.gov.ac.domain.lookup.AttachmentContentType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentContentTypeRepository extends JpaRepository<AttachmentContentType, Long> {

  Optional<AttachmentContentType> findByCodeIgnoreCaseAndActiveTrueAndDeletedAtIsNull(String code);
}
