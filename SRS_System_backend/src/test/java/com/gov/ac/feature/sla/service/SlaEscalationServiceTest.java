package com.gov.ac.feature.sla.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gov.ac.feature.audit.service.AuditTrailService;
import com.gov.ac.feature.correspondence.entity.CorrespondenceEntity;
import com.gov.ac.feature.delegation.entity.AuthorityDelegationEntity;
import com.gov.ac.feature.delegation.repository.AuthorityDelegationRepository;
import com.gov.ac.feature.departments.entity.DepartmentEntity;
import com.gov.ac.feature.departments.repository.DepartmentRepository;
import com.gov.ac.feature.lookups.entity.ConfidentialityEntity;
import com.gov.ac.feature.organization.entity.OrganizationalUnitLevelEntity;
import com.gov.ac.feature.organization.repository.OrganizationalUnitLevelRepository;
import com.gov.ac.feature.organization.service.OrgLevelRoleResolver;
import com.gov.ac.feature.sla.entity.SlaBreachEventEntity;
import com.gov.ac.feature.sla.entity.SlaEscalationStepEntity;
import com.gov.ac.feature.sla.entity.SlaPolicyEntity;
import com.gov.ac.feature.sla.metrics.SlaMetrics;
import com.gov.ac.feature.sla.notification.SlaNotifier;
import com.gov.ac.feature.sla.repository.SlaBreachEventRepository;
import com.gov.ac.feature.users.entity.AppUserEntity;
import com.gov.ac.feature.users.repository.UserRoleRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.camunda.bpm.engine.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit-level coverage of every escalation action in {@link SlaEscalationService}. The clearance
 * boundary is the load-bearing security contract; we assert that a less-cleared delegate causes a
 * reassignment to be refused even when an authority delegation exists.
 */
@ExtendWith(MockitoExtension.class)
class SlaEscalationServiceTest {

  @Mock private TaskService taskService;
  @Mock private AuthorityDelegationRepository authorityDelegationRepository;
  @Mock private DepartmentRepository departmentRepository;
  @Mock private OrganizationalUnitLevelRepository organizationalUnitLevelRepository;
  @Mock private OrgLevelRoleResolver orgLevelRoleResolver;
  @Mock private UserRoleRepository userRoleRepository;
  @Mock private SlaBreachEventRepository slaBreachEventRepository;
  @Mock private SlaClearanceFilter slaClearanceFilter;
  @Mock private SlaNotifier slaNotifier;
  @Mock private SlaMetrics slaMetrics;
  @Mock private AuditTrailService auditTrailService;

  @InjectMocks private SlaEscalationService service;

  private SlaBreachEventEntity breach;
  private SlaPolicyEntity policy;
  private SlaEscalationStepEntity step;
  private CorrespondenceEntity correspondence;

  @BeforeEach
  void setUp() {
    policy = new SlaPolicyEntity();
    policy.setId(1L);
    policy.setCode("SLA_DEFAULT");
    policy.setTargetHours(4);
    policy.setActive(true);

    DepartmentEntity ownerDept = new DepartmentEntity();
    ownerDept.setId(100L);
    ownerDept.setCode("DEPT-100");
    ownerDept.setNameAr("dep");
    ownerDept.setNameEn("dep");
    ownerDept.setLevelCode("S");

    correspondence = new CorrespondenceEntity();
    correspondence.setId(UUID.randomUUID());
    correspondence.setReferenceNumber("REF-1");
    correspondence.setSubject("Subject");
    correspondence.setOwnerDepartment(ownerDept);
    correspondence.setConfidentiality(confidentiality(50));

    breach = new SlaBreachEventEntity();
    breach.setId(7L);
    breach.setTaskId("task-1");
    breach.setProcessInstanceId("pi-1");
    breach.setCorrespondence(correspondence);
    breach.setPolicy(policy);
    breach.setBreachedAt(Instant.now());
    breach.setTargetAt(Instant.now());

    step = new SlaEscalationStepEntity();
    step.setId(700L);
    step.setPolicy(policy);
    step.setStepOrder(0);
  }

  // ---------------------------------------------------------------------------
  // NOTIFY_MANAGER
  // ---------------------------------------------------------------------------

  @Test
  void notifyManagerSendsToClearedRecipients() {
    UUID assignee = UUID.randomUUID();
    UUID manager = UUID.randomUUID();
    step.setActionCode(SlaEscalationStepEntity.ACTION_NOTIFY_MANAGER);
    lenient()
        .when(userRoleRepository.findActiveUserIdsByRoleCodeAndDepartmentId("DEPT_MANAGER", 100L))
        .thenReturn(List.of(manager));
    lenient().when(departmentRepository.findByIdAndDeletedAtIsNull(100L))
        .thenReturn(Optional.of(correspondence.getOwnerDepartment()));
    when(slaClearanceFilter.filter(eq(correspondence), any())).thenReturn(List.of(manager));

    boolean ok = service.executeStep(breach, step, correspondence, assignee.toString(), "wf");

    assertThat(ok).isTrue();
    verify(slaNotifier)
        .notifyRecipients(
            eq(List.of(manager)),
            eq(SlaNotifier.DEFAULT_EVENT_CODE),
            eq(correspondence),
            eq(SlaNotifier.MESSAGE_KEY_SLA_BREACH),
            any());
  }

  @Test
  void notifyManagerSkipsWhenNoCandidatesCleared() {
    UUID assignee = UUID.randomUUID();
    step.setActionCode(SlaEscalationStepEntity.ACTION_NOTIFY_MANAGER);
    when(slaClearanceFilter.filter(eq(correspondence), any())).thenReturn(List.of());

    boolean ok = service.executeStep(breach, step, correspondence, assignee.toString(), "wf");

    assertThat(ok).isTrue();
    verify(slaNotifier, never()).notifyRecipients(any(), any(), any(), any(), any());
  }

  // ---------------------------------------------------------------------------
  // REASSIGN_TO_DELEGATE  — confidentiality boundary
  // ---------------------------------------------------------------------------

  @Test
  void reassignToDelegateRefusesWhenDelegateNotCleared() {
    UUID assignee = UUID.randomUUID();
    UUID delegate = UUID.randomUUID();
    AuthorityDelegationEntity ad = new AuthorityDelegationEntity();
    AppUserEntity delegateUser = new AppUserEntity();
    delegateUser.setId(delegate);
    ad.setDelegateUser(delegateUser);
    step.setActionCode(SlaEscalationStepEntity.ACTION_REASSIGN_TO_DELEGATE);
    when(authorityDelegationRepository.findFirstActiveByDelegator(eq(assignee), any()))
        .thenReturn(Optional.of(ad));
    when(slaClearanceFilter.isCleared(correspondence, delegate)).thenReturn(false);

    boolean ok = service.executeStep(breach, step, correspondence, assignee.toString(), "wf");

    assertThat(ok)
        .as("Step should report success (clean no-op) so the engine advances; "
            + "the actual reassignment must not have happened")
        .isTrue();
    verify(taskService, never()).setAssignee(any(), any());
  }

  @Test
  void reassignToDelegateProceedsWhenClearanceMatches() {
    UUID assignee = UUID.randomUUID();
    UUID delegate = UUID.randomUUID();
    AuthorityDelegationEntity ad = new AuthorityDelegationEntity();
    AppUserEntity delegateUser = new AppUserEntity();
    delegateUser.setId(delegate);
    ad.setDelegateUser(delegateUser);
    step.setActionCode(SlaEscalationStepEntity.ACTION_REASSIGN_TO_DELEGATE);
    when(authorityDelegationRepository.findFirstActiveByDelegator(eq(assignee), any()))
        .thenReturn(Optional.of(ad));
    when(slaClearanceFilter.isCleared(correspondence, delegate)).thenReturn(true);

    boolean ok = service.executeStep(breach, step, correspondence, assignee.toString(), "wf");

    assertThat(ok).isTrue();
    verify(taskService).setAssignee("task-1", delegate.toString());
  }

  // ---------------------------------------------------------------------------
  // ESCALATE_TO_HIGHER_LEVEL
  // ---------------------------------------------------------------------------

  @Test
  void escalateToHigherLevelWalksParentDepartmentChain() {
    UUID assignee = UUID.randomUUID();
    UUID higherUser = UUID.randomUUID();
    DepartmentEntity higher = new DepartmentEntity();
    higher.setId(200L);
    higher.setCode("DEPT-200");
    higher.setLevelCode("K");
    higher.setNameAr("h");
    higher.setNameEn("h");
    correspondence.getOwnerDepartment().setParent(higher);

    step.setActionCode(SlaEscalationStepEntity.ACTION_ESCALATE_TO_HIGHER_LEVEL);

    when(departmentRepository.findByIdAndDeletedAtIsNull(100L))
        .thenReturn(Optional.of(correspondence.getOwnerDepartment()));
    when(organizationalUnitLevelRepository.findActiveByCode("S"))
        .thenReturn(Optional.of(level("S", 4)));
    when(organizationalUnitLevelRepository.findActiveByCode("K"))
        .thenReturn(Optional.of(level("K", 3)));
    when(orgLevelRoleResolver.resolveRoleCode("K")).thenReturn("DEPT_MANAGER");
    when(userRoleRepository.findActiveUserIdsByRoleCodeAndDepartmentId("DEPT_MANAGER", 200L))
        .thenReturn(List.of(higherUser));
    when(slaClearanceFilter.filter(eq(correspondence), any())).thenReturn(List.of(higherUser));

    boolean ok = service.executeStep(breach, step, correspondence, assignee.toString(), "wf");

    assertThat(ok).isTrue();
    verify(slaNotifier)
        .notifyRecipients(
            eq(List.of(higherUser)),
            eq(SlaNotifier.DEFAULT_EVENT_CODE),
            eq(correspondence),
            eq(SlaNotifier.MESSAGE_KEY_SLA_BREACH),
            any());
  }

  // ---------------------------------------------------------------------------
  // NOTIFY_AUDIT_ADMIN
  // ---------------------------------------------------------------------------

  @Test
  void notifyAuditAdminQueriesGlobalRoleSet() {
    UUID admin = UUID.randomUUID();
    step.setActionCode(SlaEscalationStepEntity.ACTION_NOTIFY_AUDIT_ADMIN);
    when(userRoleRepository.findActiveUserIdsByRoleCodes(List.of("SYS_ADMIN", "AUDITOR")))
        .thenReturn(List.of(admin));
    when(slaClearanceFilter.filter(eq(correspondence), any())).thenReturn(List.of(admin));

    boolean ok = service.executeStep(breach, step, correspondence, UUID.randomUUID().toString(), "wf");

    assertThat(ok).isTrue();
    verify(slaNotifier)
        .notifyRecipients(
            eq(List.of(admin)),
            eq(SlaNotifier.DEFAULT_EVENT_CODE),
            eq(correspondence),
            eq(SlaNotifier.MESSAGE_KEY_SLA_BREACH),
            any());
  }

  // ---------------------------------------------------------------------------
  // markResolved
  // ---------------------------------------------------------------------------

  @Test
  void markResolvedIsIdempotent() {
    breach.setResolvedAt(Instant.now()); // already resolved
    when(slaBreachEventRepository.findByTaskId("task-1")).thenReturn(Optional.of(breach));

    service.markResolved("task-1", "TASK_COMPLETED", "wf");

    verify(slaBreachEventRepository, never()).save(any());
    verify(slaMetrics, never()).recordBreachOutcome(any(), any());
  }

  // ---------------------------------------------------------------------------
  // helpers
  // ---------------------------------------------------------------------------

  private static ConfidentialityEntity confidentiality(int sortOrder) {
    ConfidentialityEntity c = new ConfidentialityEntity();
    c.setId((long) sortOrder);
    c.setCode("C-" + sortOrder);
    c.setNameAr("c");
    c.setNameEn("c");
    c.setSortOrder(sortOrder);
    c.setRequiresClearance(false);
    return c;
  }

  private static OrganizationalUnitLevelEntity level(String code, int rank) {
    OrganizationalUnitLevelEntity l = new OrganizationalUnitLevelEntity();
    l.setCode(code);
    l.setRankOrder(rank);
    l.setNameAr("l");
    l.setNameEn("l");
    return l;
  }
}
