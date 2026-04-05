package com.gov.ac.persistence;

import com.gov.ac.domain.auth.MfaOtpChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MfaOtpChallengeRepository extends JpaRepository<MfaOtpChallenge, Long> {}
