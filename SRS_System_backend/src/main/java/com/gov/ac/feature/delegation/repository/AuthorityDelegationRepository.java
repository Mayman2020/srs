package com.gov.ac.feature.delegation.repository;

import com.gov.ac.feature.delegation.entity.AuthorityDelegationEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthorityDelegationRepository extends JpaRepository<AuthorityDelegationEntity, UUID> {

  @EntityGraph(attributePaths = {"delegatorUser", "delegateUser"})
  @Query(
      "select d from AuthorityDelegationEntity d where d.deletedAt is null "
          + "and (d.delegatorUser.id = :uid or d.delegateUser.id = :uid) "
          + "order by d.createdAt desc")
  List<AuthorityDelegationEntity> findVisibleForUser(@Param("uid") UUID userId);

  @EntityGraph(attributePaths = {"delegatorUser", "delegateUser"})
  @Query(
      "select d from AuthorityDelegationEntity d where d.id = :id and d.deletedAt is null")
  Optional<AuthorityDelegationEntity> findByIdAndDeletedAtIsNull(@Param("id") UUID id);
}
