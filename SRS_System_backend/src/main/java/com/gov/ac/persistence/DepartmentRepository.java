package com.gov.ac.persistence;

import com.gov.ac.domain.org.Department;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

  Optional<Department> findByIdAndDeletedAtIsNull(Long id);

  @EntityGraph(attributePaths = "parent")
  List<Department> findByDeletedAtIsNullAndActiveTrueOrderBySortOrderAsc();

  boolean existsByCodeIgnoreCaseAndDeletedAtIsNull(String code);

  boolean existsByCodeIgnoreCaseAndDeletedAtIsNullAndIdNot(String code, Long id);

  List<Department> findByParent_IdAndDeletedAtIsNull(Long parentId);
}
