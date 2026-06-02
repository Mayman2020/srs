package com.gov.ac.feature.workflow.bpmn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.io.InputStream;
import java.util.List;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Smoke test: every BPMN under {@code processes/} must parse with Camunda's model API, contain
 * exactly one executable {@link Process} and at least one {@link StartEvent}. Catches obvious
 * regressions when authors edit XML by hand.
 */
class BpmnDeployabilityTest {

  @ParameterizedTest(name = "BPMN file [{0}] parses and is deployable")
  @ValueSource(
      strings = {
        "processes/inbound-correspondence.bpmn",
        "processes/internal-correspondence.bpmn",
        "processes/outbound-correspondence.bpmn"
      })
  void bpmnFileIsValid(String resourcePath) {
    try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
      assertThat(in).as("BPMN resource %s must be on the classpath", resourcePath).isNotNull();
      assertThatNoException()
          .isThrownBy(
              () -> {
                BpmnModelInstance model = Bpmn.readModelFromStream(in);
                List<Process> processes = model.getModelElementsByType(Process.class).stream().toList();
                assertThat(processes)
                    .as("BPMN %s must define at least one process", resourcePath)
                    .isNotEmpty();
                assertThat(processes.stream().anyMatch(Process::isExecutable))
                    .as("BPMN %s must contain at least one executable process", resourcePath)
                    .isTrue();
                assertThat(model.getModelElementsByType(StartEvent.class))
                    .as("BPMN %s must contain at least one start event", resourcePath)
                    .isNotEmpty();
              });
    } catch (Exception e) {
      throw new AssertionError("Failed to read BPMN resource " + resourcePath, e);
    }
  }
}
