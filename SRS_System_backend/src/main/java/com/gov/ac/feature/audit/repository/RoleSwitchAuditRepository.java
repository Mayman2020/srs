package com.gov.ac.feature.audit.repository;

import com.gov.ac.feature.audit.entity.RoleSwitchAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleSwitchAuditRepository extends JpaRepository<RoleSwitchAuditEntity, Long> {}
