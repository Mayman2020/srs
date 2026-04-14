package com.gov.ac.feature.auth.repository;

import com.gov.ac.feature.auth.entity.MfaOtpChallengeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MfaOtpChallengeRepository extends JpaRepository<MfaOtpChallengeEntity, Long> {}
