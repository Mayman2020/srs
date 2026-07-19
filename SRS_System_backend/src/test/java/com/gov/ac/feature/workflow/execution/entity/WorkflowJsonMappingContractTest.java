package com.gov.ac.feature.workflow.execution.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

class WorkflowJsonMappingContractTest {

  @Test
  void stringBackedJsonbFieldsUseHibernateJsonJdbcType() throws Exception {
    assertJsonMapping(WorkflowInstanceEntity.class, "routingChainJson");
    assertJsonMapping(WorkflowActionEntity.class, "payload");
  }

  private static void assertJsonMapping(Class<?> entity, String fieldName) throws Exception {
    JdbcTypeCode mapping = entity.getDeclaredField(fieldName).getAnnotation(JdbcTypeCode.class);
    assertThat(mapping).as(entity.getSimpleName() + "." + fieldName).isNotNull();
    assertThat(mapping.value()).isEqualTo(SqlTypes.JSON);
  }
}
