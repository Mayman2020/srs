package com.gov.ac.persistence;

import com.gov.ac.domain.user.Role;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

  List<Role> findByDeletedAtIsNullOrderBySortOrderAsc();
}
