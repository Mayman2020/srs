package com.gov.ac.feature.users.repository;

import com.gov.ac.feature.users.entity.AppUserEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppUserRepository extends JpaRepository<AppUserEntity, UUID> {

  @EntityGraph(attributePaths = "department")
  Page<AppUserEntity> findByDeletedAtIsNull(Pageable pageable);

  @EntityGraph(attributePaths = "department")
  @Query("""
      select u from AppUserEntity u
      where u.deletedAt is null
        and (
          lower(u.username) like lower(concat('%', :q, '%'))
          or lower(u.fullNameAr) like lower(concat('%', :q, '%'))
          or lower(u.fullNameEn) like lower(concat('%', :q, '%'))
          or lower(u.email) like lower(concat('%', :q, '%'))
        )
      """)
  Page<AppUserEntity> searchActiveDirectory(@Param("q") String q, Pageable pageable);

  @EntityGraph(attributePaths = "department")
  Optional<AppUserEntity> findByIdAndDeletedAtIsNull(UUID id);

  List<AppUserEntity> findByDepartment_IdAndDeletedAtIsNullAndActiveTrue(Long departmentId);

  /** Pick any active user in the given department (deterministic by created_at). */
  Optional<AppUserEntity> findFirstByDepartment_IdAndDeletedAtIsNullAndActiveTrueOrderByCreatedAtAsc(
      Long departmentId);

  /** Convenience alias used by routing stop assignment listener. */
  default Optional<AppUserEntity> findFirstActiveByDepartmentId(Long departmentId) {
    return findFirstByDepartment_IdAndDeletedAtIsNullAndActiveTrueOrderByCreatedAtAsc(departmentId);
  }

  @EntityGraph(attributePaths = "department")
  Optional<AppUserEntity> findByUsernameAndDeletedAtIsNull(String username);

  boolean existsByEmailAndDeletedAtIsNullAndIdNot(String email, UUID id);
}
