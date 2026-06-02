package com.gov.ac.feature.roles.repository;

import com.gov.ac.feature.roles.entity.PermissionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PermissionRepository extends JpaRepository<PermissionEntity, Long> {

  List<PermissionEntity> findByDeletedAtIsNullOrderBySortOrderAsc();

  Optional<PermissionEntity> findByIdAndDeletedAtIsNull(Long id);

  Optional<PermissionEntity> findByCodeIgnoreCaseAndDeletedAtIsNull(String code);

  boolean existsByCodeIgnoreCaseAndDeletedAtIsNull(String code);

  boolean existsByCodeIgnoreCaseAndDeletedAtIsNullAndIdNot(String code, Long id);

  /**
   * Resolve a permission code either by its own canonical {@code code} OR by mapping it through
   * {@code permission_alias.alias_code}. Used by {@code EffectiveUserPermissionService} so that
   * both legacy ({@code correspondence.view}, {@code VIEW_TRANSACTIONS}) and canonical
   * ({@code CORRESPONDENCE_VIEW}) codes work in {@code @PreAuthorize} expressions.
   */
  @Query(
      value =
          "SELECT p.* FROM srs_system.permission p "
              + "WHERE p.deleted_at IS NULL AND UPPER(p.code) = UPPER(:code) "
              + "UNION "
              + "SELECT p.* FROM srs_system.permission p "
              + "JOIN srs_system.permission_alias a ON a.permission_id = p.id "
              + "WHERE p.deleted_at IS NULL AND UPPER(a.alias_code) = UPPER(:code) "
              + "LIMIT 1",
      nativeQuery = true)
  Optional<PermissionEntity> findByCanonicalOrAliasCode(String code);
}
