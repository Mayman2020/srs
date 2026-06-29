package com.gov.ac.feature.organization.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.gov.ac.feature.departments.entity.DepartmentEntity;
import com.gov.ac.feature.departments.repository.DepartmentRepository;
import com.gov.ac.feature.organization.dto.RoutingChainDto;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit-tests for the Q/L/K/S routing algorithm. The department tree under test is:
 *
 * <pre>
 *   Q (id=1, HQ)
 *   ├── L1 (id=10)
 *   │   ├── K1 (id=100)
 *   │   │   └── S1 (id=1000)
 *   │   └── K2 (id=200)
 *   │       └── S2 (id=2000)
 *   └── L2 (id=20)
 *       └── K3 (id=300)
 *           └── S3 (id=3000)
 * </pre>
 */
@ExtendWith(MockitoExtension.class)
class OrgRoutingServiceTest {

  @Mock private DepartmentRepository departmentRepository;
  @Mock private OrgLevelRoleResolver orgLevelRoleResolver;

  @InjectMocks private OrgRoutingService routingService;

  private DepartmentEntity q;
  private DepartmentEntity l1;
  private DepartmentEntity l2;
  private DepartmentEntity k1;
  private DepartmentEntity k2;
  private DepartmentEntity k3;
  private DepartmentEntity s1;
  private DepartmentEntity s2;
  private DepartmentEntity s3;

  @BeforeEach
  void setUp() {
    lenient().when(orgLevelRoleResolver.resolveRoleCode(anyString())).thenReturn("DEPT_MANAGER");

    q = dept(1L, "Q-HQ", "Q", null);
    l1 = dept(10L, "L1", "L", q);
    l2 = dept(20L, "L2", "L", q);
    k1 = dept(100L, "K1", "K", l1);
    k2 = dept(200L, "K2", "K", l1);
    k3 = dept(300L, "K3", "K", l2);
    s1 = dept(1000L, "S1", "S", k1);
    s2 = dept(2000L, "S2", "S", k2);
    s3 = dept(3000L, "S3", "S", k3);

    for (DepartmentEntity d : new DepartmentEntity[] {q, l1, l2, k1, k2, k3, s1, s2, s3}) {
      lenient()
          .when(departmentRepository.findByIdAndDeletedAtIsNull(d.getId()))
          .thenReturn(Optional.of(d));
    }
  }

  @Test
  void sameBattalionSToSIsDirect() {
    DepartmentEntity peer = dept(1001L, "S1B", "S", k1);
    lenient()
        .when(departmentRepository.findByIdAndDeletedAtIsNull(peer.getId()))
        .thenReturn(Optional.of(peer));

    RoutingChainDto chain = routingService.computeChain(s1.getId(), peer.getId());

    assertThat(chain.stops()).hasSize(1);
    assertThat(chain.stops().get(0).departmentId()).isEqualTo(peer.getId());
    assertThat(chain.reasonKey()).isEqualTo("routing.sameUnit");
  }

  @Test
  void sameBrigadeDifferentBattalionsHopsThroughBothKs() {
    RoutingChainDto chain = routingService.computeChain(s1.getId(), s2.getId());

    assertThat(chain.stops())
        .extracting(stop -> stop.departmentId())
        .containsExactly(k1.getId(), k2.getId(), s2.getId());
    assertThat(chain.reasonKey()).isEqualTo("routing.viaParent");
  }

  @Test
  void differentBrigadesGoThroughHeadquarters() {
    RoutingChainDto chain = routingService.computeChain(s1.getId(), s3.getId());

    assertThat(chain.stops())
        .extracting(stop -> stop.departmentId())
        .containsExactly(k1.getId(), l1.getId(), q.getId(), l2.getId(), k3.getId(), s3.getId());
    assertThat(chain.reasonKey()).isEqualTo("routing.viaHeadquarters");
  }

  @Test
  void headquartersOriginatorReachesAnyTargetDirectly() {
    RoutingChainDto chain = routingService.computeChain(q.getId(), s3.getId());

    assertThat(chain.stops()).hasSize(1);
    assertThat(chain.stops().get(0).departmentId()).isEqualTo(s3.getId());
    assertThat(chain.reasonKey()).isEqualTo("routing.fromHeadquarters");
  }

  private static DepartmentEntity dept(Long id, String code, String level, DepartmentEntity parent) {
    DepartmentEntity d = new DepartmentEntity();
    d.setId(id);
    d.setCode(code);
    d.setNameAr(code);
    d.setNameEn(code);
    d.setLevelCode(level);
    d.setParent(parent);
    return d;
  }
}
