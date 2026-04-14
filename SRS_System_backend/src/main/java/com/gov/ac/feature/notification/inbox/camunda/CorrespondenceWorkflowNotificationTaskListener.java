package com.gov.ac.feature.notification.inbox.camunda;

import com.gov.ac.feature.shared.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.stereotype.Component;

@Component("correspondenceWorkflowNotificationTaskListener")
@RequiredArgsConstructor
public class CorrespondenceWorkflowNotificationTaskListener implements TaskListener {

  private final NotificationService notificationService;

  @Override
  public void notify(DelegateTask delegateTask) {
    notificationService.notifyWorkflowTaskCompleted(delegateTask);
  }
}
