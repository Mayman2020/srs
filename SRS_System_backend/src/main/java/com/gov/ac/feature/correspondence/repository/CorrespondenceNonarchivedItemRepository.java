package com.gov.ac.feature.correspondence.repository;

import com.gov.ac.feature.correspondence.entity.CorrespondenceNonarchivedItemEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CorrespondenceNonarchivedItemRepository
    extends JpaRepository<CorrespondenceNonarchivedItemEntity, Long> {

  @Query(
      "select i from CorrespondenceNonarchivedItemEntity i where i.correspondence.id = :cid "
          + "and i.deletedAt is null order by i.sortOrder asc, i.id asc")
  List<CorrespondenceNonarchivedItemEntity> listActiveForCorrespondence(@Param("cid") UUID correspondenceId);

  @Query(
      "select i from CorrespondenceNonarchivedItemEntity i where i.id = :id and i.correspondence.id = :cid "
          + "and i.deletedAt is null")
  Optional<CorrespondenceNonarchivedItemEntity> findActiveByIdAndCorrespondence(
      @Param("id") Long itemId, @Param("cid") UUID correspondenceId);
}
