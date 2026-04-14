package com.gov.ac.feature.users.repository;

import com.gov.ac.feature.users.entity.AppUserEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUserEntity, UUID> {

  @EntityGraph(attributePaths = "department")
  Page<AppUserEntity> findByDeletedAtIsNull(Pageable pageable);

  @EntityGraph(attributePaths = "department")
  Optional<AppUserEntity> findByIdAndDeletedAtIsNull(UUID id);

  List<AppUserEntity> findByDepartment_IdAndDeletedAtIsNullAndActiveTrue(Long departmentId);

  @EntityGraph(attributePaths = "department")
  Optional<AppUserEntity> findByUsernameAndDeletedAtIsNull(String username);

  boolean existsByEmailAndDeletedAtIsNullAndIdNot(String email, UUID id);
}
