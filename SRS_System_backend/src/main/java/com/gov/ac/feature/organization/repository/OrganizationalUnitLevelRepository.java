package com.gov.ac.feature.organization.repository;

import com.gov.ac.feature.organization.entity.OrganizationalUnitLevelEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrganizationalUnitLevelRepository
    extends JpaRepository<OrganizationalUnitLevelEntity, Long> {

  @Query(
      "SELECT l FROM OrganizationalUnitLevelEntity l "
          + "WHERE l.deletedAt IS NULL AND UPPER(l.code) = UPPER(:code)")
  Optional<OrganizationalUnitLevelEntity> findActiveByCode(String code);

  @Query(
      "SELECT l FROM OrganizationalUnitLevelEntity l "
          + "WHERE l.deletedAt IS NULL AND l.active = true ORDER BY l.rankOrder ASC")
  List<OrganizationalUnitLevelEntity> findAllActive();
}
