package com.gov.ac.persistence;

import com.gov.ac.domain.audit.RoleSwitchAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleSwitchAuditRepository extends JpaRepository<RoleSwitchAudit, Long> {}
