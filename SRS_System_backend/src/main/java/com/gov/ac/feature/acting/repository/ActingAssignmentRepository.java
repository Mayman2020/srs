package com.gov.ac.feature.acting.repository;

import com.gov.ac.feature.acting.entity.ActingAssignmentEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActingAssignmentRepository extends JpaRepository<ActingAssignmentEntity, UUID> {

  @EntityGraph(
      attributePaths = {
        "absentUser",
        "actingUser",
        "department",
        "correspondenceType",
        "confidentiality",
        "workflowActionType"
      })
  @Query(
      "select a from ActingAssignmentEntity a where a.absentUser.id = :absentId "
          + "and a.revokedAt is null and a.validFrom <= :day and a.validTo >= :day")
  List<ActingAssignmentEntity> findActiveByAbsentUser(
      @Param("absentId") UUID absentId, @Param("day") LocalDate day);

  @EntityGraph(
      attributePaths = {
        "absentUser",
        "actingUser",
        "department",
        "correspondenceType",
        "confidentiality",
        "workflowActionType"
      })
  @Query(
      "select a from ActingAssignmentEntity a where a.actingUser.id = :actingId "
          + "and a.revokedAt is null and a.validFrom <= :day and a.validTo >= :day")
  List<ActingAssignmentEntity> findActiveByActingUser(
      @Param("actingId") UUID actingId, @Param("day") LocalDate day);

  @EntityGraph(
      attributePaths = {
        "absentUser",
        "actingUser",
        "department",
        "correspondenceType",
        "confidentiality",
        "workflowActionType"
      })
  @Query(
      "select a from ActingAssignmentEntity a where a.revokedAt is null "
          + "and a.validTo < :day")
  List<ActingAssignmentEntity> findExpiredNotRevoked(@Param("day") LocalDate day);

  @EntityGraph(
      attributePaths = {
        "absentUser",
        "actingUser",
        "department",
        "correspondenceType",
        "confidentiality",
        "workflowActionType"
      })
  Optional<ActingAssignmentEntity> findByIdAndRevokedAtIsNull(UUID id);

  @EntityGraph(
      attributePaths = {
        "absentUser",
        "actingUser",
        "department",
        "correspondenceType",
        "confidentiality",
        "workflowActionType"
      })
  @Query(
      "select a from ActingAssignmentEntity a where (a.absentUser.id = :uid or a.actingUser.id = :uid) "
          + "and (a.revokedAt is not null or a.validTo < :today) "
          + "order by a.updatedAt desc")
  List<ActingAssignmentEntity> findInactiveForUser(@Param("uid") UUID uid, @Param("today") LocalDate today);

  @Query(
      "select a from ActingAssignmentEntity a where (a.absentUser.id = :uid or a.actingUser.id = :uid) "
          + "and a.revokedAt is null and a.validFrom > :today order by a.validFrom asc")
  List<ActingAssignmentEntity> findUpcomingForUser(@Param("uid") UUID uid, @Param("today") LocalDate today);
}
