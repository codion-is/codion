/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jackson.json.db;

import org.jminor.common.db.ConditionType;
import org.jminor.framework.db.condition.Conditions;
import org.jminor.framework.db.condition.PropertyCondition;
import org.jminor.framework.domain.Entity;
import org.jminor.plugin.jackson.json.TestDomain;
import org.jminor.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class ConditionObjectMapperTest {

  private final TestDomain domain = new TestDomain();

  @Test
  public void propertyConditionInteger() throws JsonProcessingException {
    final ConditionObjectMapper mapper = new ConditionObjectMapper(new EntityObjectMapper(domain));

    final PropertyCondition condition = Conditions.propertyCondition(TestDomain.DEPARTMENT_ID,
            ConditionType.NOT_LIKE, asList(1, 2, 3, 4));
    final String jsonString = mapper.writeValueAsString(condition);
    final PropertyCondition readCondition = mapper.readValue(jsonString, PropertyCondition.class);

    assertEquals(condition.getPropertyId(), readCondition.getPropertyId());
    assertEquals(condition.getConditionType(), readCondition.getConditionType());
    assertEquals(condition.getValues(), readCondition.getValues());
  }

  @Test
  public void propertyConditionEntity() throws JsonProcessingException {
    final ConditionObjectMapper mapper = new ConditionObjectMapper(new EntityObjectMapper(domain));

    final Entity dept1 = domain.entity(TestDomain.T_DEPARTMENT);
    dept1.put(TestDomain.DEPARTMENT_ID, 1);
    final Entity dept2 = domain.entity(TestDomain.T_DEPARTMENT);
    dept2.put(TestDomain.DEPARTMENT_ID, 2);

    final PropertyCondition condition = Conditions.propertyCondition(TestDomain.EMP_DEPARTMENT_FK,
            ConditionType.NOT_LIKE, asList(dept1, dept2));
    final String jsonString = mapper.writeValueAsString(condition);
    final PropertyCondition readCondition = mapper.readValue(jsonString, PropertyCondition.class);

    assertEquals(condition.getPropertyId(), readCondition.getPropertyId());
    assertEquals(condition.getConditionType(), readCondition.getConditionType());
//    assertEquals(condition.getValues(), readCondition.getValues());
  }
}
