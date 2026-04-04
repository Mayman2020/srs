package com.gov.ac.persistence;

import com.gov.ac.domain.correspondence.AttachmentVersion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentVersionRepository extends JpaRepository<AttachmentVersion, Long> {}
