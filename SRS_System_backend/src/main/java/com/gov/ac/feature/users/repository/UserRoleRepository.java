package com.gov.ac.feature.users.repository;

import com.gov.ac.feature.users.entity.UserRoleEntity;
import com.gov.ac.feature.users.entity.UserRoleId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UserRoleId> {

  @Query("select ur from UserRoleEntity ur where ur.id.appUserId = :userId")
  List<UserRoleEntity> findAllByUserId(@Param("userId") UUID userId);

  @Query(
      value =
          "select ur.role_id from srs_system.user_role ur "
              + "where ur.app_user_id = :userId "
              + "and ur.valid_from <= current_timestamp "
              + "and (ur.valid_to is null or ur.valid_to > current_timestamp)",
      nativeQuery = true)
  List<Long> findActiveRoleIdsByUserId(@Param("userId") UUID userId);
}
