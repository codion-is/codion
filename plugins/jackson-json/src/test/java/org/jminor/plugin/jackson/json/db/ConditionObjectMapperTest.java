/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jackson.json.db;

import org.jminor.common.Conjunction;
import org.jminor.common.db.ConditionType;
import org.jminor.framework.db.condition.Condition;
import org.jminor.framework.db.condition.Conditions;
import org.jminor.framework.db.condition.CustomCondition;
import org.jminor.framework.db.condition.EntityCondition;
import org.jminor.framework.domain.Entity;
import org.jminor.plugin.jackson.json.TestDomain;
import org.jminor.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class ConditionObjectMapperTest {

  private final TestDomain domain = new TestDomain();

  @Test
  public void entityCondition() throws JsonProcessingException {
    final ConditionObjectMapper mapper = new ConditionObjectMapper(new EntityObjectMapper(domain));

    final Entity dept1 = domain.entity(TestDomain.T_DEPARTMENT);
    dept1.put(TestDomain.DEPARTMENT_ID, 1);
    final Entity dept2 = domain.entity(TestDomain.T_DEPARTMENT);
    dept2.put(TestDomain.DEPARTMENT_ID, 2);

    final EntityCondition entityCondition = Conditions.entityCondition(TestDomain.T_EMP,
            Conditions.conditionSet(Conjunction.AND,
                    Conditions.propertyCondition(TestDomain.EMP_DEPARTMENT_FK,
                            ConditionType.NOT_LIKE, asList(dept1, dept2)),
                    Conditions.propertyCondition(TestDomain.EMP_NAME,
                            ConditionType.LIKE, "Loc"),
                    Conditions.propertyCondition(TestDomain.EMP_ID,
                            ConditionType.WITHIN_RANGE, asList(10, 40))));

    final String jsonString = mapper.writeValueAsString(entityCondition);
    final EntityCondition readEntityCondition = mapper.readValue(jsonString, EntityCondition.class);
    final Condition condition = entityCondition.getCondition();
    final Condition readCondition = readEntityCondition.getCondition();

    assertEquals(entityCondition.getEntityId(), readEntityCondition.getEntityId());
    assertEquals(condition.getPropertyIds(), readCondition.getPropertyIds());
    assertEquals(condition.getValues(), readCondition.getValues());

    assertEquals("((deptno not in (?, ?)) and ename = ? and (empno >= ? and empno <= ?))",
            Conditions.whereCondition(entityCondition, domain.getDefinition(TestDomain.T_EMP)).getWhereClause());
  }

  @Test
  public void customCondition() throws JsonProcessingException {
    final ConditionObjectMapper mapper = new ConditionObjectMapper(new EntityObjectMapper(domain));

    final CustomCondition condition = Conditions.customCondition(TestDomain.ENTITY_CONDITION_ID,
            asList(TestDomain.ENTITY_DECIMAL, TestDomain.ENTITY_DATE_TIME),
            asList(BigDecimal.valueOf(123.4), LocalDateTime.now()));

    final EntityCondition entityCondition = Conditions.entityCondition(TestDomain.T_ENTITY, condition);

    final String jsonString = mapper.writeValueAsString(entityCondition);
    final EntityCondition readEntityCondition = mapper.readValue(jsonString, EntityCondition.class);

    final CustomCondition readCondition = (CustomCondition) readEntityCondition.getCondition();

    assertEquals(condition.getConditionId(), readCondition.getConditionId());
    assertEquals(condition.getPropertyIds(), readCondition.getPropertyIds());
    assertEquals(condition.getValues(), readCondition.getValues());
  }
}
