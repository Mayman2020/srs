package com.gov.ac.feature.workflow.bpmn;

import static org.assertj.core.api.Assertions.assertThat;

import com.gov.ac.feature.correspondence.workflow.CorrespondenceProcessDefinitionKeys;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Business-level contract for the three Q/L/K/S correspondence workflows. */
class BpmnSemanticContractTest {

  static List<String> processKeys() {
    return List.of(
        CorrespondenceProcessDefinitionKeys.INBOUND,
        CorrespondenceProcessDefinitionKeys.OUTBOUND,
        CorrespondenceProcessDefinitionKeys.INTERNAL);
  }

  @ParameterizedTest
  @MethodSource("processKeys")
  void workflowContainsRequiredRoutingDecisionTimerAndPersistenceContracts(String processKey)
      throws Exception {
    Document document = read("processes/" + processKey + ".bpmn");
    Element process = only(document, "process");
    assertThat(process.getAttribute("id")).isEqualTo(processKey);
    assertThat(process.getAttribute("isExecutable")).isEqualTo("true");

    Element multi = only(document, "multiInstanceLoopCharacteristics");
    assertThat(multi.getAttribute("isSequential")).isEqualTo("true");
    assertThat(multi.getAttributeNS("http://camunda.org/schema/1.0/bpmn", "collection"))
        .isEqualTo("${routingStops}");
    assertThat(text(only(document, "completionCondition")))
        .contains("chainExitDecisionCodes", "wfDecision");

    // Q/L/K/S is intentionally sequential. Parallel gateways would change the approved model.
    assertThat(document.getElementsByTagNameNS("*", "parallelGateway").getLength()).isZero();

    NodeList userTasks = document.getElementsByTagNameNS("*", "userTask");
    assertThat(userTasks.getLength()).isGreaterThanOrEqualTo(2);
    for (int i = 0; i < userTasks.getLength(); i++) {
      Element task = (Element) userTasks.item(i);
      Set<String> listeners = listenerExpressions(task);
      assertThat(listeners)
          .contains(
              "${correspondenceWorkflowPersistenceTaskListener}",
              "${correspondenceWorkflowNotificationTaskListener}");
    }

    Element boundary = only(document, "boundaryEvent");
    assertThat(boundary.getAttribute("cancelActivity")).isEqualTo("false");
    assertThat(text(only(document, "timeDuration")))
        .contains("routingStop.slaIso", "defaultSlaIso");
    assertThat(delegateExpressions(document, "serviceTask"))
        .contains("${routingChainDelegate}", "${workflowTimerEscalationDelegate}");

    Element gateway = only(document, "exclusiveGateway");
    assertThat(gateway.getAttribute("default")).isNotBlank();
    List<String> conditions = elements(document, "conditionExpression").stream()
        .map(BpmnSemanticContractTest::text)
        .toList();
    assertThat(conditions).anyMatch(v -> v.contains("rejectDecisionCodes"));
    assertThat(conditions).anyMatch(v -> v.contains("returnDecisionCodes"));

    NodeList endEvents = document.getElementsByTagNameNS("*", "endEvent");
    long finalEnds = elements(document, "endEvent").stream()
        .filter(e -> listenerExpressions(e).contains("${finalStatusExecutionListener}"))
        .count();
    assertThat(endEvents.getLength()).isGreaterThanOrEqualTo(4);
    assertThat(finalEnds).isEqualTo(3);
  }

  private static Document read(String path) throws Exception {
    try (InputStream in = BpmnSemanticContractTest.class.getClassLoader().getResourceAsStream(path)) {
      assertThat(in).as(path).isNotNull();
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      return factory.newDocumentBuilder().parse(in);
    }
  }

  private static Element only(Document document, String localName) {
    NodeList nodes = document.getElementsByTagNameNS("*", localName);
    assertThat(nodes.getLength()).as(localName).isEqualTo(1);
    return (Element) nodes.item(0);
  }

  private static List<Element> elements(Document document, String localName) {
    NodeList nodes = document.getElementsByTagNameNS("*", localName);
    return java.util.stream.IntStream.range(0, nodes.getLength())
        .mapToObj(i -> (Element) nodes.item(i))
        .toList();
  }

  private static Set<String> listenerExpressions(Element parent) {
    NodeList nodes = parent.getElementsByTagNameNS("http://camunda.org/schema/1.0/bpmn", "taskListener");
    if (nodes.getLength() == 0) {
      nodes = parent.getElementsByTagNameNS("http://camunda.org/schema/1.0/bpmn", "executionListener");
    }
    Set<String> result = new java.util.HashSet<>();
    for (int i = 0; i < nodes.getLength(); i++) {
      result.add(((Element) nodes.item(i)).getAttribute("delegateExpression"));
    }
    return result;
  }

  private static Set<String> delegateExpressions(Document document, String localName) {
    return elements(document, localName).stream()
        .map(e -> e.getAttributeNS("http://camunda.org/schema/1.0/bpmn", "delegateExpression"))
        .filter(v -> !v.isBlank())
        .collect(java.util.stream.Collectors.toSet());
  }

  private static String text(Element element) {
    return element.getTextContent().trim();
  }
}
