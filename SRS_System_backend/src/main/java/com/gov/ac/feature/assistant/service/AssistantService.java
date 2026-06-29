package com.gov.ac.feature.assistant.service;

import com.gov.ac.feature.assistant.dto.AssistantActionDto;
import com.gov.ac.feature.assistant.dto.AssistantAnswerResponseDto;
import com.gov.ac.feature.dashboard.dto.DashboardResponseDto;
import com.gov.ac.feature.dashboard.service.DashboardService;
import com.gov.ac.feature.workflow.execution.service.WorkflowTaskInboxService;
import com.gov.ac.config.AssistantProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssistantService {

  private final DashboardService dashboardService;
  private final WorkflowTaskInboxService workflowTaskInboxService;
  private final AssistantLlmClient llmClient;
  private final AssistantProperties properties;

  @Transactional(readOnly = true)
  public AssistantAnswerResponseDto answer(UUID userId, String query) {
    String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    DashboardResponseDto dash = dashboardService.getDashboard();
    int inboxCount = workflowTaskInboxService.listMyOpenTasks(userId, 200).size();

    List<AssistantActionDto> actions = buildActions(normalized, dash, inboxCount);
    String context =
        """
        Live SRS correspondence metrics:
        - Total correspondences: %d
        - In progress: %d
        - Overdue: %d
        - Inbound: %d
        - Outbound: %d
        - User open workflow tasks: %d
        """
            .formatted(
                dash.totalCorrespondences(),
                dash.kpiPipelineCount(),
                dash.overdueCount(),
                dash.kpiInboxCount(),
                dash.kpiOutboundCount(),
                inboxCount);

    String ruleText = buildRuleText(normalized, dash, inboxCount);
    String llmText = null;
    if (properties.isLlmEnabled()) {
      String system =
          """
          You are a government correspondence (SRS) business assistant. Answer in the same language as the user \
          (Arabic or English). Use only the provided live metrics and suggest navigation actions when helpful. \
          Be concise (max 6 sentences). Do not invent numbers.
          """;
      llmText = llmClient.complete(system, context + "\n\nUser question:\n" + query);
    }

    String text = llmText != null && !llmText.isBlank() ? llmText : ruleText;
    return new AssistantAnswerResponseDto(text, actions, llmText != null);
  }

  private static String buildRuleText(String normalized, DashboardResponseDto dash, int inboxCount) {
    if (normalized.contains("تسجيل") || normalized.contains("مكتب") || normalized.contains("registration")) {
      return "افتح مكتب التسجيل لتسجيل الوارد/الصادر وطباعة ملصقات الباركود.";
    }
    if (normalized.contains("مهام") || normalized.contains("صندوق") || normalized.contains("workflow")
        || normalized.contains("task")) {
      return "لديك " + inboxCount + " مهمة مفتوحة في صندوق سير العمل.";
    }
    if (normalized.contains("صادر") || normalized.contains("outbound") || normalized.contains("تسليم")) {
      return "تتبع تسليم الصادر من شاشة تتبع الصادر أو من تبويب التسليم داخل المعاملة الصادرة.";
    }
    if (normalized.contains("متأخر") || normalized.contains("overdue") || normalized.contains("late")) {
      return "يوجد " + dash.overdueCount() + " معاملة متأخرة حالياً.";
    }
    if (normalized.contains("معلق") || normalized.contains("pending") || normalized.contains("قيد")) {
      return "يوجد " + dash.kpiPipelineCount() + " معاملة قيد التنفيذ.";
    }
    return "النظام يتتبع "
        + dash.totalCorrespondences()
        + " معاملة، منها "
        + dash.overdueCount()
        + " متأخرة و"
        + inboxCount
        + " مهمة في صندوقك.";
  }

  private static List<AssistantActionDto> buildActions(
      String normalized, DashboardResponseDto dash, int inboxCount) {
    List<AssistantActionDto> actions = new ArrayList<>();
    if (normalized.contains("تسجيل") || normalized.contains("registration")) {
      actions.add(new AssistantActionDto("reg", "مكتب التسجيل", "/registration-desk", null));
    }
    if (normalized.contains("مهام") || normalized.contains("workflow") || normalized.contains("task")
        || inboxCount > 0) {
      actions.add(new AssistantActionDto("tasks", "صندوق المهام", "/workflow-tasks", null));
    }
    if (normalized.contains("صادر") || normalized.contains("outbound")) {
      actions.add(new AssistantActionDto("outbound", "تتبع الصادر", "/outbound-delivery", null));
    }
    if (dash.overdueCount() > 0) {
      actions.add(new AssistantActionDto("reports", "التقارير", "/reports", null));
    }
    actions.add(new AssistantActionDto("correspondence", "المعاملات", "/correspondence", null));
    return actions;
  }
}
