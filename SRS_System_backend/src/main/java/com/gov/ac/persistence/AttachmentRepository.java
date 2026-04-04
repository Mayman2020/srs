package com.gov.ac.persistence;

import com.gov.ac.domain.correspondence.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {}
