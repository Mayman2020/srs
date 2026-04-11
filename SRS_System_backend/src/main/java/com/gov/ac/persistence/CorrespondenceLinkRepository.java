package com.gov.ac.persistence;

import com.gov.ac.domain.correspondence.CorrespondenceLink;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CorrespondenceLinkRepository extends JpaRepository<CorrespondenceLink, Long> {

  @EntityGraph(attributePaths = {"linkedCorrespondence"})
  @Query(
      "select l from CorrespondenceLink l where l.correspondence.id = :cid and l.deletedAt is null "
          + "order by l.createdAt desc, l.id desc")
  List<CorrespondenceLink> listActiveForCorrespondence(@Param("cid") UUID correspondenceId);

  @Query(
      "select count(l) > 0 from CorrespondenceLink l where l.correspondence.id = :a "
          + "and l.linkedCorrespondence.id = :b and l.deletedAt is null")
  boolean existsActivePair(
      @Param("a") UUID correspondenceId, @Param("b") UUID linkedCorrespondenceId);

  @EntityGraph(attributePaths = {"correspondence", "linkedCorrespondence"})
  @Query(
      "select l from CorrespondenceLink l where l.id = :id and l.correspondence.id = :cid "
          + "and l.deletedAt is null")
  Optional<CorrespondenceLink> findActiveByIdAndCorrespondence(
      @Param("id") Long linkId, @Param("cid") UUID correspondenceId);
}
