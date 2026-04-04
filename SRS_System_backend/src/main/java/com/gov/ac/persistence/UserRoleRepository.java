package com.gov.ac.persistence;

import com.gov.ac.domain.user.UserRole;
import com.gov.ac.domain.user.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {}
