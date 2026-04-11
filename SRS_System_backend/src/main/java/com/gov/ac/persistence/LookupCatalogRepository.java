package com.gov.ac.persistence;

import com.gov.ac.domain.lookup.LookupCatalog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LookupCatalogRepository extends JpaRepository<LookupCatalog, String> {

  List<LookupCatalog> findAllByOrderBySortOrderAsc();
}
