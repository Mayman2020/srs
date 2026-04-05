package com.gov.ac.persistence;

import com.gov.ac.domain.user.Role;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleRepository extends JpaRepository<Role, Long> {

  List<Role> findByDeletedAtIsNullOrderBySortOrderAsc();

  Optional<Role> findByIdAndDeletedAtIsNullAndActiveTrue(Long id);

  Optional<Role> findByCodeIgnoreCaseAndDeletedAtIsNullAndActiveTrue(String code);

  @Query(
      value =
          "select r.code from role r "
              + "inner join user_role ur on ur.role_id = r.id "
              + "where ur.app_user_id = :userId "
              + "and r.deleted_at is null "
              + "and r.is_active = true "
              + "and ur.valid_from <= current_timestamp "
              + "and (ur.valid_to is null or ur.valid_to > current_timestamp)",
      nativeQuery = true)
  List<String> findActiveRoleCodesByUserId(@Param("userId") UUID userId);
}
