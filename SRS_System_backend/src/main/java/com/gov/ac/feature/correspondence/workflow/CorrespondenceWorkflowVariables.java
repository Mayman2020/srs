package com.gov.ac.feature.correspondence.workflow;

/** Process variables for first user-task assignment (Camunda). */
public final class CorrespondenceWorkflowVariables {

  private CorrespondenceWorkflowVariables() {}

  public static final String INITIATOR = "initiator";

  /**
   * Optional UUID string: first task is assigned to this user instead of initiator.
   */
  public static final String WF_FIRST_ASSIGNEE_USER_ID = "wfFirstAssigneeUserId";

  /**
   * Optional {@code role.code} (e.g. CORRESP_CLERK): first task is a candidate group task — any user
   * with that role may claim it.
   */
  public static final String WF_FIRST_CANDIDATE_GROUP = "wfFirstCandidateGroup";
}
