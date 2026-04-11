package com.gov.ac.persistence;

import com.gov.ac.domain.leave.LeaveRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {

  @Query(
      "select l from LeaveRequest l join fetch l.user left join fetch l.decidedBy "
          + "where l.id = :id and l.deletedAt is null")
  Optional<LeaveRequest> findByIdAndDeletedAtIsNull(@Param("id") UUID id);

  @Query(
      "select l from LeaveRequest l join fetch l.user left join fetch l.decidedBy "
          + "where l.user.id = :uid and l.deletedAt is null order by l.createdAt desc")
  List<LeaveRequest> findByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(@Param("uid") UUID userId);

  @Query(
      "select l from LeaveRequest l join fetch l.user left join fetch l.decidedBy "
          + "where l.deletedAt is null order by l.createdAt desc")
  List<LeaveRequest> findByDeletedAtIsNullOrderByCreatedAtDesc();
}
