package com.gov.ac.feature.leave.repository;

import com.gov.ac.feature.leave.entity.LeaveRequestEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequestEntity, UUID> {

  @Query(
      "select l from LeaveRequestEntity l join fetch l.user join fetch l.status "
          + "left join fetch l.decidedBy where l.id = :id and l.deletedAt is null")
  Optional<LeaveRequestEntity> findByIdAndDeletedAtIsNull(@Param("id") UUID id);

  @Query(
      "select l from LeaveRequestEntity l join fetch l.user join fetch l.status "
          + "left join fetch l.decidedBy "
          + "where l.user.id = :uid and l.deletedAt is null order by l.createdAt desc")
  List<LeaveRequestEntity> findByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(@Param("uid") UUID userId);

  @Query(
      "select l from LeaveRequestEntity l join fetch l.user join fetch l.status "
          + "left join fetch l.decidedBy "
          + "where l.deletedAt is null order by l.createdAt desc")
  List<LeaveRequestEntity> findByDeletedAtIsNullOrderByCreatedAtDesc();
}
