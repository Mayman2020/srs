package com.gov.ac.feature.correspondence.outbound.repository;

import com.gov.ac.feature.correspondence.outbound.entity.CorrespondenceOutboundDeliveryEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CorrespondenceOutboundDeliveryRepository
    extends JpaRepository<CorrespondenceOutboundDeliveryEntity, Long> {

  @Query(
      "select d from CorrespondenceOutboundDeliveryEntity d "
          + "join fetch d.correspondence c "
          + "where d.deletedAt is null order by d.updatedAt desc")
  List<CorrespondenceOutboundDeliveryEntity> findAllActive();

  @Query(
      "select d from CorrespondenceOutboundDeliveryEntity d "
          + "join fetch d.correspondence c "
          + "where d.deletedAt is null and c.id = :correspondenceId "
          + "order by d.updatedAt desc")
  List<CorrespondenceOutboundDeliveryEntity> findActiveByCorrespondenceId(
      @Param("correspondenceId") UUID correspondenceId);

  @Query(
      "select d from CorrespondenceOutboundDeliveryEntity d "
          + "join fetch d.correspondence c "
          + "where d.id = :id and d.deletedAt is null")
  Optional<CorrespondenceOutboundDeliveryEntity> findActiveById(@Param("id") Long id);
}
