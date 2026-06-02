package com.gov.ac.feature.attachment.verification.repository;

import com.gov.ac.feature.attachment.verification.entity.AttachmentVerificationAccessLogEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentVerificationAccessLogRepository
    extends JpaRepository<AttachmentVerificationAccessLogEntity, UUID> {}
