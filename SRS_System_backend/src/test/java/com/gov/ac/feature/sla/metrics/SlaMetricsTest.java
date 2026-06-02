package com.gov.ac.feature.sla.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.gov.ac.feature.sla.repository.SlaBreachEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Prometheus-facing assertions: every counter/gauge registers under the documented name and the
 * gauge tracks the unresolved breach count. Pins the metric contract referenced by Grafana
 * dashboards and Prometheus alert rules.
 */
@ExtendWith(MockitoExtension.class)
class SlaMetricsTest {

  @Mock private SlaBreachEventRepository slaBreachEventRepository;

  private SimpleMeterRegistry meterRegistry;
  private SlaMetrics metrics;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    when(slaBreachEventRepository.countUnresolved()).thenReturn(0L);
    metrics = new SlaMetrics(meterRegistry, slaBreachEventRepository);
    // PostConstruct is normally called by Spring; the unit test does it manually.
    metrics.registerOverdueGauge();
  }

  @Test
  void breachCounterUsesCanonicalNameAndTags() {
    metrics.recordBreachOutcome(SlaMetrics.OUTCOME_BREACH_DETECTED, "inbound-correspondence");

    double counterValue =
        meterRegistry
            .counter(
                SlaMetrics.METRIC_SLA_BREACH,
                "outcome",
                SlaMetrics.OUTCOME_BREACH_DETECTED,
                "process",
                "inbound-correspondence")
            .count();

    assertThat(counterValue).isEqualTo(1.0);
  }

  @Test
  void escalationCounterUsesCanonicalNameAndActionTag() {
    metrics.recordEscalationStep("NOTIFY_MANAGER", "inbound-correspondence");
    metrics.recordEscalationStep("NOTIFY_MANAGER", "inbound-correspondence");

    double counterValue =
        meterRegistry
            .counter(
                SlaMetrics.METRIC_SLA_ESCALATION,
                "action",
                "NOTIFY_MANAGER",
                "process",
                "inbound-correspondence")
            .count();

    assertThat(counterValue).isEqualTo(2.0);
  }

  @Test
  void overdueGaugeReflectsRepositoryCount() {
    when(slaBreachEventRepository.countUnresolved()).thenReturn(7L);

    metrics.refreshOverdueGauge();

    double gaugeValue =
        meterRegistry.find(SlaMetrics.METRIC_SLA_OVERDUE_ACTIVE).gauge().value();
    assertThat(gaugeValue).isEqualTo(7.0);
    assertThat(metrics.currentOverdueActive()).isEqualTo(7L);
  }
}
