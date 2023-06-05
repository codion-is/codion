/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.CustomCondition;
import is.codion.framework.db.condition.SelectCondition;
import is.codion.framework.db.condition.UpdateCondition;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.json.TestDomain;
import is.codion.framework.json.TestDomain.Department;
import is.codion.framework.json.TestDomain.Employee;
import is.codion.framework.json.TestDomain.TestEntity;
import is.codion.framework.json.domain.EntityObjectMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public final class ConditionObjectMapperTest {

  private final Entities entities = new TestDomain().entities();

  @Test
  void condition() throws JsonProcessingException {
    ConditionObjectMapper mapper = ConditionObjectMapper.conditionObjectMapper(EntityObjectMapper.entityObjectMapper(entities));

    Entity dept1 = entities.builder(Department.TYPE)
            .with(Department.DEPTNO, 1)
            .build();
    Entity dept2 = entities.builder(Department.TYPE)
            .with(Department.DEPTNO, 2)
            .build();

    Condition entityCondition = Condition.where(Employee.DEPARTMENT_FK).notEqualTo(dept1, dept2).and(
            Condition.where(Employee.NAME).equalTo("Loc"),
            Condition.where(Employee.EMPNO).between(10, 40),
            Condition.where(Employee.COMMISSION).isNotNull());

    String jsonString = mapper.writeValueAsString(entityCondition);
    Condition readCondition = mapper.readValue(jsonString, Condition.class);

    assertEquals(entityCondition.entityType(), readCondition.entityType());
    assertEquals(entityCondition.attributes(), readCondition.attributes());
    assertEquals(entityCondition.values(), readCondition.values());

    assertEquals("(deptno not in (?, ?) and ename = ? and (empno >= ? and empno <= ?) and comm is not null)",
            entityCondition.toString(entities.definition(Employee.TYPE)));
  }

  @Test
  void nullCondition() throws JsonProcessingException {
    ConditionObjectMapper mapper = ConditionObjectMapper.conditionObjectMapper(EntityObjectMapper.entityObjectMapper(entities));
    Condition entityCondition = Condition.where(Employee.COMMISSION).isNotNull();

    String jsonString = mapper.writeValueAsString(entityCondition);
    Condition readCondition = mapper.readValue(jsonString, Condition.class);

    assertEquals(entityCondition.entityType(), readCondition.entityType());
    assertEquals(entityCondition.attributes(), readCondition.attributes());
    assertEquals(entityCondition.values(), readCondition.values());
  }

  @Test
  void customCondition() throws JsonProcessingException {
    ConditionObjectMapper mapper = ConditionObjectMapper.conditionObjectMapper(EntityObjectMapper.entityObjectMapper(entities));

    CustomCondition condition = Condition.customCondition(TestEntity.CONDITION_TYPE,
            asList(TestEntity.DECIMAL, TestEntity.DATE_TIME),
            asList(BigDecimal.valueOf(123.4), LocalDateTime.now()));

    String jsonString = mapper.writeValueAsString(condition);
    CustomCondition readCondition = (CustomCondition) mapper.readValue(jsonString, Condition.class);

    assertEquals(condition.conditionType(), readCondition.conditionType());
    assertEquals(condition.attributes(), readCondition.attributes());
    assertEquals(condition.values(), readCondition.values());
  }

  @Test
  void selectCondition() throws JsonProcessingException {
    ConditionObjectMapper mapper = ConditionObjectMapper.conditionObjectMapper(EntityObjectMapper.entityObjectMapper(entities));

    SelectCondition selectCondition = Condition.where(Employee.EMPNO).equalTo(1)
            .selectBuilder()
            .orderBy(OrderBy.builder()
                    .ascending(Employee.EMPNO)
                    .descendingNullsLast(Employee.NAME)
                    .ascendingNullsFirst(Employee.JOB)
                    .build())
            .limit(2)
            .offset(1)
            .forUpdate()
            .queryTimeout(42)
            .fetchDepth(2)
            .fetchDepth(Employee.DEPARTMENT_FK, 0)
            .selectAttributes(Employee.COMMISSION, Employee.DEPARTMENT)
            .build();

    String jsonString = mapper.writeValueAsString(selectCondition);
    SelectCondition readCondition = mapper.readValue(jsonString, SelectCondition.class);

    assertEquals(selectCondition.condition().attributes(), readCondition.condition().attributes());
    assertEquals(selectCondition.orderBy().orElse(null).orderByAttributes(), readCondition.orderBy().get().orderByAttributes());
    assertEquals(selectCondition.limit(), readCondition.limit());
    assertEquals(selectCondition.offset(), readCondition.offset());
    assertEquals(selectCondition.fetchDepth().orElse(null), readCondition.fetchDepth().orElse(null));
    for (ForeignKey foreignKey : entities.definition(selectCondition.entityType()).foreignKeys()) {
      assertEquals(selectCondition.fetchDepth(foreignKey), readCondition.fetchDepth(foreignKey));
    }
    assertEquals(selectCondition.selectAttributes(), readCondition.selectAttributes());
    assertTrue(readCondition.forUpdate());
    assertEquals(42, readCondition.queryTimeout());
    assertEquals(selectCondition, readCondition);

    selectCondition = Condition.where(Employee.EMPNO).equalTo(1).selectBuilder().build();

    jsonString = mapper.writeValueAsString(selectCondition);
    readCondition = mapper.readValue(jsonString, SelectCondition.class);

    assertFalse(readCondition.orderBy().isPresent());
    assertFalse(readCondition.fetchDepth().isPresent());

    Condition condition = Condition.where(Employee.EMPNO).equalTo(2);
    jsonString = mapper.writeValueAsString(condition);

    selectCondition = mapper.readValue(jsonString, SelectCondition.class);
  }

  @Test
  void updateCondition() throws JsonProcessingException {
    ConditionObjectMapper mapper = ConditionObjectMapper.conditionObjectMapper(EntityObjectMapper.entityObjectMapper(entities));

    UpdateCondition condition = Condition.where(Department.DEPTNO)
            .between(1, 2).updateBuilder()
            .set(Department.LOCATION, "loc")
            .set(Department.DEPTNO, 3)
            .build();

    String jsonString = mapper.writeValueAsString(condition);
    UpdateCondition readCondition = mapper.readValue(jsonString, UpdateCondition.class);

    assertEquals(condition.condition().attributes(), readCondition.condition().attributes());
    assertEquals(condition.condition().values(), readCondition.condition().values());
    assertEquals(condition.attributeValues(), readCondition.attributeValues());
  }
}
