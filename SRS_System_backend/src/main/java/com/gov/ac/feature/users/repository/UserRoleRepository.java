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

  /**
   * Single-query effective permission id union for a user: joins {@code user_role -> role ->
   * role_permission -> permission} and filters by temporal validity, role active/non-deleted, and
   * permission active/non-deleted. This is the canonical authorization source consumed by {@code
   * EffectiveUserPermissionService}; the union is intentionally computed at the DB layer so that
   * deactivating a role or soft-deleting a permission removes capability immediately, with no
   * application-side filtering required.
   */
  @Query(
      value =
          "select distinct p.id from srs_system.user_role ur "
              + "join srs_system.role r on r.id = ur.role_id "
              + "join srs_system.role_permission rp on rp.role_id = r.id "
              + "join srs_system.permission p on p.id = rp.permission_id "
              + "where ur.app_user_id = :userId "
              + "and ur.valid_from <= current_timestamp "
              + "and (ur.valid_to is null or ur.valid_to > current_timestamp) "
              + "and r.deleted_at is null and r.is_active = true "
              + "and p.deleted_at is null and p.is_active = true",
      nativeQuery = true)
  List<Long> findEffectivePermissionIdsByUserId(@Param("userId") UUID userId);

  /**
   * Effective permission ids for {@code userId} plus every permission granted to <em>absent</em>
   * users for whom {@code userId} is the acting manager under a date-active {@code acting_assignment}
   * whose optional {@code department_id} is NULL or equals the acting user's {@code department_id}.
   */
  @Query(
      value =
          "select distinct p.id from srs_system.permission p "
              + "where p.deleted_at is null and p.is_active = true "
              + "and p.id in ("
              + "select p2.id from srs_system.user_role ur "
              + "join srs_system.role r on r.id = ur.role_id "
              + "join srs_system.role_permission rp on rp.role_id = r.id "
              + "join srs_system.permission p2 on p2.id = rp.permission_id "
              + "where ur.app_user_id = :userId "
              + "and ur.valid_from <= current_timestamp "
              + "and (ur.valid_to is null or ur.valid_to > current_timestamp) "
              + "and r.deleted_at is null and r.is_active = true "
              + "and p2.deleted_at is null and p2.is_active = true "
              + "union "
              + "select p3.id from srs_system.user_role ur2 "
              + "join srs_system.role r3 on r3.id = ur2.role_id "
              + "join srs_system.role_permission rp3 on rp3.role_id = r3.id "
              + "join srs_system.permission p3 on p3.id = rp3.permission_id "
              + "join srs_system.acting_assignment aa on aa.absent_user_id = ur2.app_user_id "
              + "join srs_system.app_user actor on actor.id = :userId "
              + "where aa.acting_user_id = :userId "
              + "and aa.revoked_at is null "
              + "and aa.valid_from <= current_date "
              + "and aa.valid_to >= current_date "
              + "and (aa.department_id is null or aa.department_id = actor.department_id) "
              + "and ur2.valid_from <= current_timestamp "
              + "and (ur2.valid_to is null or ur2.valid_to > current_timestamp) "
              + "and r3.deleted_at is null and r3.is_active = true "
              + "and p3.deleted_at is null and p3.is_active = true"
              + ")",
      nativeQuery = true)
  List<Long> findEffectivePermissionIdsByUserIdIncludingActing(@Param("userId") UUID userId);

  /**
   * Find active user ids holding the role identified by {@code roleCode} whose home department
   * matches {@code departmentId}. Used by the routing stop assignment listener to resolve a
   * concrete assignee for the next routing stop.
   */
  @Query(
      value =
          "select u.id from srs_system.user_role ur "
              + "join srs_system.role r on r.id = ur.role_id and r.deleted_at is null "
              + "join srs_system.app_user u on u.id = ur.app_user_id "
              + "where upper(r.code) = upper(:roleCode) "
              + "and u.department_id = :departmentId "
              + "and u.deleted_at is null and u.is_active = true "
              + "and ur.valid_from <= current_timestamp "
              + "and (ur.valid_to is null or ur.valid_to > current_timestamp) "
              + "order by u.created_at asc "
              + "limit 5",
      nativeQuery = true)
  List<UUID> findActiveUserIdsByRoleCodeAndDepartmentId(
      @Param("roleCode") String roleCode, @Param("departmentId") Long departmentId);

  /**
   * Active users holding any of the supplied role codes across every department. Used by SLA
   * escalation steps that need to reach audit / administration roles regardless of where the
   * breach occurred (e.g. {@code NOTIFY_AUDIT_ADMIN}). Capped at 50 to keep notification fan-out
   * bounded.
   */
  @Query(
      value =
          "select distinct u.id from srs_system.user_role ur "
              + "join srs_system.role r on r.id = ur.role_id and r.deleted_at is null "
              + "join srs_system.app_user u on u.id = ur.app_user_id "
              + "where upper(r.code) in (:roleCodes) "
              + "and u.deleted_at is null and u.is_active = true "
              + "and ur.valid_from <= current_timestamp "
              + "and (ur.valid_to is null or ur.valid_to > current_timestamp) "
              + "order by u.id "
              + "limit 50",
      nativeQuery = true)
  List<UUID> findActiveUserIdsByRoleCodes(@Param("roleCodes") List<String> roleCodes);
}
