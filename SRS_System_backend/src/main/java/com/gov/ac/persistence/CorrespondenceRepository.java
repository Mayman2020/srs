package com.gov.ac.persistence;

import com.gov.ac.domain.correspondence.Correspondence;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CorrespondenceRepository extends JpaRepository<Correspondence, UUID> {

  @EntityGraph(
      attributePaths = {"correspondenceType", "correspondenceStatus", "priority", "classification"})
  Page<Correspondence> findByDeletedAtIsNull(Pageable pageable);

  @EntityGraph(
      attributePaths = {"correspondenceType", "correspondenceStatus", "priority", "classification"})
  Optional<Correspondence> findByIdAndDeletedAtIsNull(UUID id);

  @Query("select count(c) from Correspondence c where c.deletedAt is null")
  long countActive();

  @Query(
      "select count(c) from Correspondence c where c.deletedAt is null and c.correspondenceType.code = :code")
  long countActiveByTypeCode(@Param("code") String code);

  @Query(
      "select count(c) from Correspondence c where c.deletedAt is null and c.correspondenceStatus.code = :code")
  long countActiveByStatusCode(@Param("code") String code);

  @Query(
      "select s.code, count(c) from Correspondence c join c.correspondenceStatus s "
          + "where c.deletedAt is null group by s.code order by min(s.sortOrder)")
  List<Object[]> countGroupedByCorrespondenceStatus();

  @Query(
      "select t.code, count(c) from Correspondence c join c.correspondenceType t "
          + "where c.deletedAt is null group by t.code order by min(t.sortOrder)")
  List<Object[]> countGroupedByCorrespondenceType();

  @Query(
      "select p.code, count(c) from Correspondence c join c.priority p "
          + "where c.deletedAt is null group by p.code order by min(p.sortOrder)")
  List<Object[]> countGroupedByPriority();
}
