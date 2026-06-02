package com.gov.ac.feature.delegation.task.repository;

import com.gov.ac.feature.delegation.task.entity.TaskDelegationEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskDelegationRepository extends JpaRepository<TaskDelegationEntity, UUID> {

  @EntityGraph(attributePaths = {"delegatorUser", "delegateUser"})
  Optional<TaskDelegationEntity> findByIdAndRevokedAtIsNull(UUID id);

  /** Rows where the given user is either the delegator or the delegate, newest first. */
  @EntityGraph(attributePaths = {"delegatorUser", "delegateUser"})
  @Query(
      "select d from TaskDelegationEntity d "
          + "where (d.delegatorUser.id = :uid or d.delegateUser.id = :uid) "
          + "order by coalesce(d.revokedAt, d.updatedAt) desc, d.createdAt desc")
  List<TaskDelegationEntity> findVisibleForUser(@Param("uid") UUID userId);

  /** Currently-active outgoing delegations for {@code delegatorId} on the given calendar day. */
  @EntityGraph(attributePaths = {"delegatorUser", "delegateUser"})
  @Query(
      "select d from TaskDelegationEntity d "
          + "where d.revokedAt is null "
          + "and d.delegatorUser.id = :delegatorId "
          + "and d.validFrom <= :day and d.validTo >= :day "
          + "order by d.createdAt desc")
  List<TaskDelegationEntity> findActiveByDelegator(
      @Param("delegatorId") UUID delegatorId, @Param("day") LocalDate day);

  /** Currently-active incoming delegations for {@code delegateId} on the given calendar day. */
  @EntityGraph(attributePaths = {"delegatorUser", "delegateUser"})
  @Query(
      "select d from TaskDelegationEntity d "
          + "where d.revokedAt is null "
          + "and d.delegateUser.id = :delegateId "
          + "and d.validFrom <= :day and d.validTo >= :day "
          + "order by d.createdAt desc")
  List<TaskDelegationEntity> findActiveByDelegate(
      @Param("delegateId") UUID delegateId, @Param("day") LocalDate day);

  /** Specific-task delegation lookup used by the Camunda assignment listener. */
  @EntityGraph(attributePaths = {"delegatorUser", "delegateUser"})
  @Query(
      "select d from TaskDelegationEntity d "
          + "where d.revokedAt is null "
          + "and d.scopeType = 'TASK' "
          + "and d.delegatorUser.id = :delegatorId "
          + "and (d.camundaTaskId = :taskId or d.correspondenceId = :correspondenceId) "
          + "and d.validFrom <= :day and d.validTo >= :day "
          + "order by case when d.camundaTaskId = :taskId then 0 else 1 end, d.createdAt desc")
  List<TaskDelegationEntity> findActiveTaskScoped(
      @Param("delegatorId") UUID delegatorId,
      @Param("taskId") String camundaTaskId,
      @Param("correspondenceId") UUID correspondenceId,
      @Param("day") LocalDate day);

  /**
   * Returns every delegation about to expire by {@code day} (inclusive) that has not been revoked
   * yet. Used by the idempotent expiry job.
   */
  @EntityGraph(attributePaths = {"delegatorUser", "delegateUser"})
  @Query(
      "select d from TaskDelegationEntity d "
          + "where d.revokedAt is null and d.validTo < :day")
  List<TaskDelegationEntity> findExpiredAsOf(@Param("day") LocalDate day);

  /**
   * Existing active delegation rows for the given delegator that overlap the window
   * {@code [validFrom, validTo]}. Caller decides whether the new request is a duplicate (same
   * delegate / scope) or a legitimate concurrent delegation to a different delegate.
   */
  @EntityGraph(attributePaths = {"delegatorUser", "delegateUser"})
  @Query(
      "select d from TaskDelegationEntity d "
          + "where d.revokedAt is null "
          + "and d.delegatorUser.id = :delegatorId "
          + "and d.validFrom <= :validTo and d.validTo >= :validFrom")
  List<TaskDelegationEntity> findOverlappingByDelegator(
      @Param("delegatorId") UUID delegatorId,
      @Param("validFrom") LocalDate validFrom,
      @Param("validTo") LocalDate validTo);
}
