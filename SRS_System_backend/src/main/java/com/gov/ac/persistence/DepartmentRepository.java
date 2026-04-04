package com.gov.ac.persistence;

import com.gov.ac.domain.org.Department;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

  Optional<Department> findByIdAndDeletedAtIsNull(Long id);
}
