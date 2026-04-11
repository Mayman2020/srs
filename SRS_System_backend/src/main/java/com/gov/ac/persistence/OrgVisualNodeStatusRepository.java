package com.gov.ac.persistence;

import com.gov.ac.domain.lookup.OrgVisualNodeStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgVisualNodeStatusRepository extends JpaRepository<OrgVisualNodeStatus, Long> {

  List<OrgVisualNodeStatus> findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc();

  List<OrgVisualNodeStatus> findByDeletedAtIsNullOrderBySortOrderAsc();

  Optional<OrgVisualNodeStatus> findByIdAndDeletedAtIsNull(Long id);

  boolean existsByCodeIgnoreCaseAndDeletedAtIsNull(String code);

  boolean existsByCodeIgnoreCaseAndIdNotAndDeletedAtIsNull(String code, Long id);
}
