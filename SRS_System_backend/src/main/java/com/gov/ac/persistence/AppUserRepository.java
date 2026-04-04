package com.gov.ac.persistence;

import com.gov.ac.domain.user.AppUser;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

  @EntityGraph(attributePaths = "department")
  Page<AppUser> findByDeletedAtIsNull(Pageable pageable);

  @EntityGraph(attributePaths = "department")
  Optional<AppUser> findByIdAndDeletedAtIsNull(UUID id);

  List<AppUser> findByDepartment_IdAndDeletedAtIsNullAndActiveTrue(Long departmentId);

  Optional<AppUser> findByUsernameIgnoreCaseAndDeletedAtIsNull(String username);
}

