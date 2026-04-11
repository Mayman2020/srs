package com.gov.ac.persistence;

import com.gov.ac.domain.correspondence.CorrespondenceNonarchivedItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CorrespondenceNonarchivedItemRepository
    extends JpaRepository<CorrespondenceNonarchivedItem, Long> {

  @Query(
      "select i from CorrespondenceNonarchivedItem i where i.correspondence.id = :cid "
          + "and i.deletedAt is null order by i.sortOrder asc, i.id asc")
  List<CorrespondenceNonarchivedItem> listActiveForCorrespondence(@Param("cid") UUID correspondenceId);

  @Query(
      "select i from CorrespondenceNonarchivedItem i where i.id = :id and i.correspondence.id = :cid "
          + "and i.deletedAt is null")
  Optional<CorrespondenceNonarchivedItem> findActiveByIdAndCorrespondence(
      @Param("id") Long itemId, @Param("cid") UUID correspondenceId);
}
