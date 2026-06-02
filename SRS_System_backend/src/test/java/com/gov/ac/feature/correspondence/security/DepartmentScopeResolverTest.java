package com.gov.ac.feature.correspondence.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.gov.ac.feature.departments.entity.DepartmentEntity;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.AppUserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DepartmentScopeResolverTest {

  @Mock private AppUserRepository appUserRepository;
  @Mock private CorrespondencePrivilegedRoleChecker privilegedRoleChecker;

  @InjectMocks private DepartmentScopeResolver resolver;

  private UUID userId;
  private AppUserEntity user;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    user = new AppUserEntity();
    DepartmentEntity dept = new DepartmentEntity();
    dept.setId(42L);
    user.setDepartment(dept);
    lenient().when(appUserRepository.findByIdAndDeletedAtIsNull(eq(userId))).thenReturn(Optional.of(user));
  }

  @Test
  void privilegedUserReceivesNullScope() {
    when(privilegedRoleChecker.hasPrivilegedViewRole(userId)).thenReturn(true);

    Long scope = resolver.resolveDepartmentScope(userId);

    assertThat(scope).isNull();
  }

  @Test
  void nonPrivilegedUserReceivesOwnDepartment() {
    when(privilegedRoleChecker.hasPrivilegedViewRole(userId)).thenReturn(false);

    Long scope = resolver.resolveDepartmentScope(userId);

    assertThat(scope).isEqualTo(42L);
  }

  @Test
  void unknownUserFallsBackToNullScope() {
    UUID otherUser = UUID.randomUUID();
    when(privilegedRoleChecker.hasPrivilegedViewRole(otherUser)).thenReturn(false);
    when(appUserRepository.findByIdAndDeletedAtIsNull(otherUser)).thenReturn(Optional.empty());

    Long scope = resolver.resolveDepartmentScope(otherUser);

    assertThat(scope).isNull();
  }
}
