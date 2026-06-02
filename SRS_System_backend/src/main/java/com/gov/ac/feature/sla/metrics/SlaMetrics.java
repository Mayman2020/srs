package com.gov.ac.feature.sla.metrics;

import com.gov.ac.feature.sla.repository.SlaBreachEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Central Micrometer registration for the SLA Policy Engine. Counters are extensions of the
 * existing {@code correspondence_sla_breach_total} metric introduced by the workflow escalation
 * scheduler, with new outcome tags. A gauge tracks unresolved {@link
 * com.gov.ac.feature.sla.entity.SlaBreachEventEntity} rows so the overdue-active count is
 * available in Prometheus without scraping Camunda.
 *
 * <p>Counters are looked up (not re-registered) on every increment by Micrometer's internal
 * map; we cache the most common ones at startup for hot-path increments.
 */
@Component
@RequiredArgsConstructor
public class SlaMetrics {

  public static final String METRIC_SLA_BREACH = "correspondence_sla_breach_total";
  public static final String METRIC_SLA_ESCALATION = "correspondence_sla_escalation_total";
  public static final String METRIC_SLA_OVERDUE_ACTIVE = "correspondence_sla_overdue_active";

  public static final String OUTCOME_BREACH_DETECTED = "breach_detected";
  public static final String OUTCOME_BREACH_RESOLVED = "breach_resolved";

  private final MeterRegistry meterRegistry;
  private final SlaBreachEventRepository slaBreachEventRepository;

  /**
   * Last observed unresolved-count, refreshed by {@link #refreshOverdueGauge()} on every job
   * tick. The gauge reads this value rather than running a count query on every Prometheus
   * scrape.
   */
  private final AtomicLong overdueActive = new AtomicLong(0);

  @PostConstruct
  void registerOverdueGauge() {
    Gauge.builder(METRIC_SLA_OVERDUE_ACTIVE, overdueActive, AtomicLong::doubleValue)
        .description("Current number of SLA breach events that have not yet been resolved.")
        .strongReference(true)
        .register(meterRegistry);
    refreshOverdueGauge();
  }

  /** Refreshes the overdue-active gauge from the database. Called by the evaluation job. */
  public void refreshOverdueGauge() {
    try {
      overdueActive.set(slaBreachEventRepository.countUnresolved());
    } catch (RuntimeException ignored) {
      // Don't let a metric refresh break the scheduler; the gauge keeps the last value.
    }
  }

  /**
   * Increments {@code correspondence_sla_breach_total{outcome=...,process=...}}. Tags are derived
   * from the breach event's process; pass {@code "unknown"} if the process key is not available.
   */
  public void recordBreachOutcome(String outcome, String processKey) {
    breachCounter(outcome, processKey).increment();
  }

  /**
   * Increments {@code correspondence_sla_escalation_total{action=...,process=...}}. Called each
   * time the evaluation job actually fires an escalation step (not when a step is skipped due to
   * a missing target).
   */
  public void recordEscalationStep(String actionCode, String processKey) {
    escalationCounter(actionCode, processKey).increment();
  }

  /** Test helper: exposes the last gauge value without touching the DB. */
  public long currentOverdueActive() {
    return overdueActive.get();
  }

  private Counter breachCounter(String outcome, String processKey) {
    return Counter.builder(METRIC_SLA_BREACH)
        .description("Correspondence SLA breaches detected, escalated, or resolved")
        .tag("outcome", outcome == null ? "unknown" : outcome)
        .tag("process", processKey == null ? "unknown" : processKey)
        .register(meterRegistry);
  }

  private Counter escalationCounter(String actionCode, String processKey) {
    return Counter.builder(METRIC_SLA_ESCALATION)
        .description("SLA escalation steps that have fired")
        .tag("action", actionCode == null ? "unknown" : actionCode)
        .tag("process", processKey == null ? "unknown" : processKey)
        .register(meterRegistry);
  }
}
