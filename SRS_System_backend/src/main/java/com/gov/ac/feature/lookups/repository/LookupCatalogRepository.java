package com.gov.ac.feature.lookups.repository;

import com.gov.ac.feature.lookups.entity.LookupCatalogEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LookupCatalogRepository extends JpaRepository<LookupCatalogEntity, String> {

  List<LookupCatalogEntity> findAllByOrderBySortOrderAsc();

  List<LookupCatalogEntity> findByParentCatalog_LookupCodeOrderBySortOrderAsc(String parentLookupCode);
}
