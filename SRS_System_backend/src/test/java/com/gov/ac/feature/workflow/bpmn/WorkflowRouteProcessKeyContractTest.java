package com.gov.ac.feature.workflow.bpmn;

import static org.assertj.core.api.Assertions.assertThat;

import com.gov.ac.feature.correspondence.workflow.CorrespondenceProcessDefinitionKeys;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Prevents DB-configured routes from referring to a process definition that is not deployed. */
class WorkflowRouteProcessKeyContractTest {

  private static final Pattern KEY =
      Pattern.compile("(?i)process_definition_key\\s*=\\s*'([^']+)'|SELECT\\s+[^;]*?'((?:inbound|outbound|internal)-correspondence)'");

  @Test
  void everySeededCorrespondenceProcessKeyHasADeployedBpmn() throws IOException {
    Set<String> configured = new HashSet<>();
    Path migrations = Path.of("src/main/resources/db/migration");
    try (var files = Files.list(migrations)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".sql")).toList()) {
        var matcher = KEY.matcher(Files.readString(file));
        while (matcher.find()) {
          String value = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
          if (value != null && value.endsWith("-correspondence")) configured.add(value);
        }
      }
    }

    Set<String> deployed = Set.of(
        CorrespondenceProcessDefinitionKeys.INBOUND,
        CorrespondenceProcessDefinitionKeys.OUTBOUND,
        CorrespondenceProcessDefinitionKeys.INTERNAL);
    assertThat(configured).containsAll(deployed).isSubsetOf(deployed);
    for (String key : configured) {
      assertThat(getClass().getClassLoader().getResource("processes/" + key + ".bpmn"))
          .as("BPMN for configured process key %s", key)
          .isNotNull();
    }
  }
}
