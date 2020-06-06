/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.db;

import is.codion.common.Conjunction;
import is.codion.common.db.Operator;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.Conditions;
import is.codion.framework.db.condition.CustomCondition;
import is.codion.framework.db.condition.EntityCondition;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.plugin.jackson.json.TestDomain;
import is.codion.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static is.codion.framework.db.condition.Conditions.condition;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class ConditionObjectMapperTest {

  private final Entities entities = new TestDomain().getEntities();

  @Test
  public void entityCondition() throws JsonProcessingException {
    final ConditionObjectMapper mapper = new ConditionObjectMapper(new EntityObjectMapper(entities));

    final Entity dept1 = entities.entity(TestDomain.T_DEPARTMENT);
    dept1.put(TestDomain.DEPARTMENT_ID, 1);
    final Entity dept2 = entities.entity(TestDomain.T_DEPARTMENT);
    dept2.put(TestDomain.DEPARTMENT_ID, 2);

    final EntityCondition entityCondition = condition(TestDomain.T_EMP,
            Conditions.combination(Conjunction.AND,
                    Conditions.attributeCondition(TestDomain.EMP_DEPARTMENT_FK,
                            Operator.NOT_LIKE, dept1, dept2),
                    Conditions.attributeCondition(TestDomain.EMP_NAME,
                            Operator.LIKE, "Loc"),
                    Conditions.attributeCondition(TestDomain.EMP_ID,
                            Operator.WITHIN_RANGE, 10, 40),
                    Conditions.attributeCondition(TestDomain.EMP_COMMISSION,
                            Operator.NOT_LIKE, null)));

    final String jsonString = mapper.writeValueAsString(entityCondition);
    final EntityCondition readEntityCondition = mapper.readValue(jsonString, EntityCondition.class);
    final Condition condition = entityCondition.getCondition();
    final Condition readCondition = readEntityCondition.getCondition();

    assertEquals(entityCondition.getEntityType(), readEntityCondition.getEntityType());
    assertEquals(condition.getAttributes(), readCondition.getAttributes());
    assertEquals(condition.getValues(), readCondition.getValues());

    assertEquals("(deptno not in (?, ?) and ename = ? and (empno >= ? and empno <= ?) and comm is not null)",
            Conditions.whereCondition(entityCondition, entities.getDefinition(TestDomain.T_EMP)).getWhereClause());
  }

  @Test
  public void nullCondition() throws JsonProcessingException {
    final ConditionObjectMapper mapper = new ConditionObjectMapper(new EntityObjectMapper(entities));
    final EntityCondition entityCondition = condition(TestDomain.T_EMP,
            Conditions.attributeCondition(TestDomain.EMP_COMMISSION, Operator.NOT_LIKE, null));

    final String jsonString = mapper.writeValueAsString(entityCondition);
    final EntityCondition readEntityCondition = mapper.readValue(jsonString, EntityCondition.class);

    final Condition condition = entityCondition.getCondition();
    final Condition readCondition = readEntityCondition.getCondition();

    assertEquals(entityCondition.getEntityType(), readEntityCondition.getEntityType());
    assertEquals(condition.getAttributes(), readCondition.getAttributes());
    assertEquals(condition.getValues(), readCondition.getValues());
  }

  @Test
  public void customCondition() throws JsonProcessingException {
    final ConditionObjectMapper mapper = new ConditionObjectMapper(new EntityObjectMapper(entities));

    final CustomCondition condition = Conditions.customCondition(TestDomain.ENTITY_CONDITION_ID,
            asList(TestDomain.ENTITY_DECIMAL, TestDomain.ENTITY_DATE_TIME),
            asList(BigDecimal.valueOf(123.4), LocalDateTime.now()));

    final EntityCondition entityCondition = condition(TestDomain.T_ENTITY, condition);

    final String jsonString = mapper.writeValueAsString(entityCondition);
    final EntityCondition readEntityCondition = mapper.readValue(jsonString, EntityCondition.class);

    final CustomCondition readCondition = (CustomCondition) readEntityCondition.getCondition();

    assertEquals(condition.getConditionId(), readCondition.getConditionId());
    assertEquals(condition.getAttributes(), readCondition.getAttributes());
    assertEquals(condition.getValues(), readCondition.getValues());
  }
}
