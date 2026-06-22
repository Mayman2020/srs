package com.gov.ac.feature.correspondence;

/**
 * Stable lookup {@code code} values expected in the database (Flyway seeds). Resolved at runtime via
 * {@link com.gov.ac.feature.shared.lookup.service.LookupResolutionService} — these constants only avoid typos and document
 * the contract with migrations; they are not an alternate source of truth.
 */
public final class CorrespondenceLookupCodes {

  private CorrespondenceLookupCodes() {}

  /** {@code correspondence_status} — initial lifecycle row for newly created correspondence. */
  public static final String INITIAL_CORRESPONDENCE_STATUS = "NEW";

  /** {@code workflow_instance_status} — Camunda bridge row while the process is active. */
  public static final String WORKFLOW_INSTANCE_RUNNING = "RUNNING";

  public static final String WORKFLOW_INSTANCE_TERMINATED = "TERMINATED";

  /** {@code workflow_instance_status} — Camunda bridge row after normal process completion. */
  public static final String WORKFLOW_INSTANCE_COMPLETED = "COMPLETED";

  /**
   * {@code workflow_history_event_type} and {@code workflow_action_type} — first timeline entry for
   * create (see Flyway V8).
   */
  public static final String WORKFLOW_HISTORY_CREATE = "CREATE";
}
