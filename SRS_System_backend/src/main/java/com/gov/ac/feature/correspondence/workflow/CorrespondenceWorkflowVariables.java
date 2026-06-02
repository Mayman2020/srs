package com.gov.ac.feature.correspondence.workflow;

/** Process variables shared by every correspondence BPMN. */
public final class CorrespondenceWorkflowVariables {

  private CorrespondenceWorkflowVariables() {}

  public static final String INITIATOR = "initiator";

  /** Optional UUID string: first task is assigned to this user instead of initiator. */
  public static final String WF_FIRST_ASSIGNEE_USER_ID = "wfFirstAssigneeUserId";

  /**
   * Optional {@code role.code} (e.g. CORRESP_CLERK): first task is a candidate group task — any user
   * with that role may claim it.
   */
  public static final String WF_FIRST_CANDIDATE_GROUP = "wfFirstCandidateGroup";

  /** Correspondence UUID (string). Set on every process start by CamundaCorrespondenceWorkflowService. */
  public static final String CORRESPONDENCE_ID = "correspondenceId";

  /** Originator user's department id (Long). Drives Q/L/K/S routing chain resolution. */
  public static final String ORIGINATOR_DEPARTMENT_ID = "originatorDepartmentId";

  /** Target department id (Long). Drives Q/L/K/S routing chain resolution. */
  public static final String TARGET_DEPARTMENT_ID = "targetDepartmentId";

  /**
   * JSON array of routing stops produced by {@code RoutingChainDelegate}. Each element matches
   * {@link com.gov.ac.feature.organization.dto.RoutingStopDto}.
   */
  public static final String ROUTING_CHAIN_JSON = "routingChainJson";

  /** Driver list for the multi-instance routing subprocess. */
  public static final String ROUTING_STOPS = "routingStops";

  /** Current routing stop (Map) bound to {@code routingStop} by the multi-instance loop. */
  public static final String ROUTING_STOP = "routingStop";

  /** Decision string for the gateway after the user task (matches {@code workflow_action_type.code}). */
  public static final String WF_DECISION = "wfDecision";

  /** Free-text comment attached to the decision (may be required by some action types). */
  public static final String ACTION_COMMENT = "actionComment";

  // ---------------------------------------------------------------------------
  // Task-delegation tracking (Slice 2). These variables are written by the
  // delegation-aware assignment listeners whenever a task is rewired to a
  // delegate, so the persistence listener can preserve the original assignee in
  // workflow_history.
  // ---------------------------------------------------------------------------

  /** UUID string of the user the task would have been assigned to in absence of a delegation. */
  public static final String ORIGINAL_ASSIGNEE_USER_ID = "originalAssigneeUserId";

  /** UUID string of the user currently holding the task as a delegate. */
  public static final String ACTING_DELEGATE_USER_ID = "actingDelegateUserId";

  /** UUID string of the {@code task_delegation} row that rewired the task. */
  public static final String TASK_DELEGATION_ID = "taskDelegationId";

  // ---------------------------------------------------------------------------
  // Acting manager (Slice 4). Written before delegation locals when an acting
  // assignment rewires the task to the acting user while the absent user remains
  // the workflow nominal assignee for audit.
  // ---------------------------------------------------------------------------

  /** UUID string of the listener's first resolved assignee (before acting / delegation). */
  public static final String WORKFLOW_DIRECT_ASSIGNEE_USER_ID = "workflowDirectAssigneeUserId";

  /** UUID string of the {@code acting_assignment} row that rewired the task. */
  public static final String ACTING_ASSIGNMENT_ID = "actingAssignmentId";

  /** UUID string of the absent (nominal) user when acting coverage applied. */
  public static final String ACTING_FOR_ABSENT_USER_ID = "actingForAbsentUserId";

  /** UUID string of the acting user who holds the task when acting coverage applied. */
  public static final String ACTING_MANAGER_USER_ID = "actingManagerUserId";
}
