/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.db;

import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.Conditions;
import is.codion.framework.db.condition.CustomCondition;
import is.codion.framework.db.condition.SelectCondition;
import is.codion.framework.db.condition.UpdateCondition;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.plugin.jackson.json.TestDomain;
import is.codion.plugin.jackson.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public final class ConditionObjectMapperTest {

  private final Entities entities = new TestDomain().getEntities();

  @Test
  void condition() throws JsonProcessingException {
    ConditionObjectMapper mapper = new ConditionObjectMapper(EntityObjectMapper.createEntityObjectMapper(entities));

    Entity dept1 = entities.builder(TestDomain.T_DEPARTMENT)
            .with(TestDomain.DEPARTMENT_ID, 1)
            .build();
    Entity dept2 = entities.builder(TestDomain.T_DEPARTMENT)
            .with(TestDomain.DEPARTMENT_ID, 2)
            .build();

    Condition entityCondition = Conditions.where(TestDomain.EMP_DEPARTMENT_FK).notEqualTo(dept1, dept2)
                    .and(Conditions.where(TestDomain.EMP_NAME).equalTo("Loc"),
                    Conditions.where(TestDomain.EMP_ID).between(10, 40),
                    Conditions.where(TestDomain.EMP_COMMISSION).isNotNull());

    String jsonString = mapper.writeValueAsString(entityCondition);
    Condition readCondition = mapper.readValue(jsonString, Condition.class);

    assertEquals(entityCondition.getEntityType(), readCondition.getEntityType());
    assertEquals(entityCondition.getAttributes(), readCondition.getAttributes());
    assertEquals(entityCondition.getValues(), readCondition.getValues());

    assertEquals("(deptno not in (?, ?) and ename = ? and (empno >= ? and empno <= ?) and comm is not null)",
            entityCondition.getConditionString(entities.getDefinition(TestDomain.T_EMP)));
  }

  @Test
  void nullCondition() throws JsonProcessingException {
    ConditionObjectMapper mapper = new ConditionObjectMapper(EntityObjectMapper.createEntityObjectMapper(entities));
    Condition entityCondition = Conditions.where(TestDomain.EMP_COMMISSION).isNotNull();

    String jsonString = mapper.writeValueAsString(entityCondition);
    Condition readCondition = mapper.readValue(jsonString, Condition.class);

    assertEquals(entityCondition.getEntityType(), readCondition.getEntityType());
    assertEquals(entityCondition.getAttributes(), readCondition.getAttributes());
    assertEquals(entityCondition.getValues(), readCondition.getValues());
  }

  @Test
  void customCondition() throws JsonProcessingException {
    ConditionObjectMapper mapper = new ConditionObjectMapper(EntityObjectMapper.createEntityObjectMapper(entities));

    CustomCondition condition = Conditions.customCondition(TestDomain.ENTITY_CONDITION_TYPE,
            asList(TestDomain.ENTITY_DECIMAL, TestDomain.ENTITY_DATE_TIME),
            asList(BigDecimal.valueOf(123.4), LocalDateTime.now()));

    String jsonString = mapper.writeValueAsString(condition);
    CustomCondition readCondition = (CustomCondition) mapper.readValue(jsonString, Condition.class);

    assertEquals(condition.getConditionType(), readCondition.getConditionType());
    assertEquals(condition.getAttributes(), readCondition.getAttributes());
    assertEquals(condition.getValues(), readCondition.getValues());
  }

  @Test
  void selectCondition() throws JsonProcessingException {
    ConditionObjectMapper mapper = new ConditionObjectMapper(EntityObjectMapper.createEntityObjectMapper(entities));

    SelectCondition selectCondition = Conditions.where(TestDomain.EMP_ID).equalTo(1)
            .toSelectCondition()
            .orderBy(OrderBy.orderBy().ascending(TestDomain.EMP_ID).descending(TestDomain.EMP_NAME))
            .limit(2)
            .offset(1)
            .forUpdate()
            .queryTimeout(42)
            .fetchDepth(2)
            .fetchDepth(TestDomain.EMP_DEPARTMENT_FK, 0)
            .selectAttributes(TestDomain.EMP_COMMISSION, TestDomain.EMP_DEPARTMENT);

    String jsonString = mapper.writeValueAsString(selectCondition);
    SelectCondition readCondition = mapper.readValue(jsonString, SelectCondition.class);

    assertEquals(selectCondition.getCondition().getAttributes(), readCondition.getCondition().getAttributes());
    assertEquals(selectCondition.getOrderBy().getOrderByAttributes(), readCondition.getOrderBy().getOrderByAttributes());
    assertEquals(selectCondition.getLimit(), readCondition.getLimit());
    assertEquals(selectCondition.getOffset(), readCondition.getOffset());
    assertEquals(selectCondition.getFetchDepth(), readCondition.getFetchDepth());
    for (ForeignKey foreignKey : entities.getDefinition(selectCondition.getEntityType()).getForeignKeys()) {
      assertEquals(selectCondition.getFetchDepth(foreignKey), readCondition.getFetchDepth(foreignKey));
    }
    assertEquals(selectCondition.getSelectAttributes(), readCondition.getSelectAttributes());
    assertTrue(readCondition.isForUpdate());
    assertEquals(42, readCondition.getQueryTimeout());

    selectCondition = Conditions.where(TestDomain.EMP_ID).equalTo(1).toSelectCondition();

    jsonString = mapper.writeValueAsString(selectCondition);
    readCondition = mapper.readValue(jsonString, SelectCondition.class);

    assertNull(readCondition.getOrderBy());
    assertNull(readCondition.getFetchDepth());

    Condition condition = Conditions.where(TestDomain.EMP_ID).equalTo(2);
    jsonString = mapper.writeValueAsString(condition);

    selectCondition = mapper.readValue(jsonString, SelectCondition.class);
  }

  @Test
  void updateCondition() throws JsonProcessingException {
    ConditionObjectMapper mapper = new ConditionObjectMapper(EntityObjectMapper.createEntityObjectMapper(entities));

    UpdateCondition condition = Conditions.where(TestDomain.DEPARTMENT_ID)
            .between(1, 2).toUpdateCondition()
            .set(TestDomain.DEPARTMENT_LOCATION, "loc")
            .set(TestDomain.DEPARTMENT_ID, 3);

    String jsonString = mapper.writeValueAsString(condition);
    UpdateCondition readCondition = mapper.readValue(jsonString, UpdateCondition.class);

    assertEquals(condition.getCondition().getAttributes(), readCondition.getCondition().getAttributes());
    assertEquals(condition.getCondition().getValues(), readCondition.getCondition().getValues());
    assertEquals(condition.getAttributeValues(), readCondition.getAttributeValues());
  }
}
