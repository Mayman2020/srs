package com.gov.ac.correspondence.workflow;

import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.stereotype.Component;

/**
 * Runs before notification listener; persists DB timeline for user task completion.
 *
 * @see CorrespondenceWorkflowTaskPersistenceService
 */
@Component("correspondenceWorkflowPersistenceTaskListener")
@RequiredArgsConstructor
public class CorrespondenceWorkflowPersistenceTaskListener implements TaskListener {

  private final CorrespondenceWorkflowTaskPersistenceService persistenceService;

  @Override
  public void notify(DelegateTask delegateTask) {
    if (!TaskListener.EVENTNAME_COMPLETE.equals(delegateTask.getEventName())) {
      return;
    }
    persistenceService.recordUserTaskCompleted(delegateTask);
  }
}
