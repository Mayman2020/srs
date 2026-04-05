package com.gov.ac.persistence;

import com.gov.ac.domain.user.UserRole;
import com.gov.ac.domain.user.UserRoleId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

  @Query(
      value =
          "select ur.role_id from user_role ur "
              + "where ur.app_user_id = :userId "
              + "and ur.valid_from <= current_timestamp "
              + "and (ur.valid_to is null or ur.valid_to > current_timestamp)",
      nativeQuery = true)
  List<Long> findActiveRoleIdsByUserId(@Param("userId") UUID userId);
}
