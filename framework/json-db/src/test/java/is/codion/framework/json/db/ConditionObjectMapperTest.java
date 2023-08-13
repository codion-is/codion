/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.Select;
import is.codion.framework.db.Update;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.CustomCondition;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.json.TestDomain;
import is.codion.framework.json.TestDomain.Department;
import is.codion.framework.json.TestDomain.Employee;
import is.codion.framework.json.TestDomain.TestEntity;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static is.codion.framework.db.condition.Condition.*;
import static is.codion.framework.json.db.ConditionObjectMapper.conditionObjectMapper;
import static is.codion.framework.json.domain.EntityObjectMapper.entityObjectMapper;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public final class ConditionObjectMapperTest {

  private final Entities entities = new TestDomain().entities();
  private final ConditionObjectMapper mapper = conditionObjectMapper(entityObjectMapper(entities));

  @Test
  void condition() throws JsonProcessingException {
    Entity dept1 = entities.builder(Department.TYPE)
            .with(Department.DEPTNO, 1)
            .build();
    Entity dept2 = entities.builder(Department.TYPE)
            .with(Department.DEPTNO, 2)
            .build();

    Select select = Select.where(and(
                    foreignKey(Employee.DEPARTMENT_FK).notIn(dept1, dept2),
                    column(Employee.NAME).equalToIgnoreCase("Loc"),
                    column(Employee.EMPNO).between(10, 40),
                    column(Employee.COMMISSION).isNotNull()))
            .build();

    String jsonString = mapper.writeValueAsString(select);
    Select readCondition = mapper.readValue(jsonString, Select.class);

    assertEquals(select, readCondition);
    assertEquals("(deptno not in (?, ?) and upper(ename) = upper(?) and (empno >= ? and empno <= ?) and comm is not null)",
            select.condition().toString(entities.definition(Employee.TYPE)));
  }

  @Test
  void nullCondition() throws JsonProcessingException {
    Condition condition = column(Employee.COMMISSION).isNotNull();

    String jsonString = mapper.writeValueAsString(condition);
    Condition readCondition = mapper.readValue(jsonString, Condition.class);

    assertEquals(condition.entityType(), readCondition.entityType());
    assertEquals(condition.columns(), readCondition.columns());
    assertEquals(condition.values(), readCondition.values());
  }

  @Test
  void custom() throws JsonProcessingException {
    CustomCondition customedCondition = customCondition(TestEntity.CONDITION_TYPE,
            asList(TestEntity.DECIMAL, TestEntity.DATE_TIME),
            asList(BigDecimal.valueOf(123.4), LocalDateTime.now()));
    Select select = Select.where(customedCondition).build();

    String jsonString = mapper.writeValueAsString(select);
    Select readCondition = mapper.readValue(jsonString, Select.class);
    CustomCondition readCustom = (CustomCondition) readCondition.condition();

    assertEquals(customedCondition.conditionType(), readCustom.conditionType());
    assertEquals(customedCondition.columns(), readCustom.columns());
    assertEquals(customedCondition.values(), readCustom.values());
  }

  @Test
  void selectCondition() throws JsonProcessingException {
    Select select = Select.where(column(Employee.EMPNO).equalTo(1))
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
            .attributes(Employee.COMMISSION, Employee.DEPARTMENT)
            .build();

    String jsonString = mapper.writeValueAsString(select);
    Select readCondition = mapper.readValue(jsonString, Select.class);

    assertEquals(select.condition(), readCondition.condition());
    assertEquals(select.orderBy().orElse(null).orderByColumns(), readCondition.orderBy().get().orderByColumns());
    assertEquals(select.limit(), readCondition.limit());
    assertEquals(select.offset(), readCondition.offset());
    assertEquals(select.fetchDepth().orElse(null), readCondition.fetchDepth().orElse(null));
    for (ForeignKey foreignKey : entities.definition(select.condition().entityType()).foreignKeys()) {
      assertEquals(select.foreignKeyFetchDepths().get(foreignKey), readCondition.foreignKeyFetchDepths().get(foreignKey));
    }
    assertEquals(select.attributes(), readCondition.attributes());
    assertTrue(readCondition.forUpdate());
    assertEquals(42, readCondition.queryTimeout());
    assertEquals(select, readCondition);

    select = Select.where(column(Employee.EMPNO).equalTo(1)).build();

    jsonString = mapper.writeValueAsString(select);
    readCondition = mapper.readValue(jsonString, Select.class);

    assertFalse(readCondition.orderBy().isPresent());
    assertFalse(readCondition.fetchDepth().isPresent());

    select = Select.where(column(Employee.EMPNO).equalTo(2)).build();
    jsonString = mapper.writeValueAsString(select);

    select = mapper.readValue(jsonString, Select.class);
  }

  @Test
  void updateCondition() throws JsonProcessingException {
    Update update = Update.where(column(Department.DEPTNO)
                    .between(1, 2))
            .set(Department.LOCATION, "loc")
            .set(Department.DEPTNO, 3)
            .build();

    String jsonString = mapper.writeValueAsString(update);
    Update readCondition = mapper.readValue(jsonString, Update.class);

    assertEquals(update.condition(), readCondition.condition());
    assertEquals(update.columnValues(), readCondition.columnValues());
  }

  @Test
  void allCondition() throws JsonProcessingException {
    Select select = Select.all(Department.TYPE).build();

    String jsonString = mapper.writeValueAsString(select);
    Select readCondition = mapper.readValue(jsonString, Select.class);

    assertEquals(select, readCondition);
  }

  @Test
  void combinationOfCombinations() throws JsonProcessingException {
    Select select = Select.where(and(
                    column(Employee.COMMISSION).equalTo(100d),
                    or(column(Employee.JOB).notEqualTo("test"),
                            column(Employee.JOB).isNotNull())))
            .build();

    String jsonString = mapper.writeValueAsString(select);
    Select readCondition = mapper.readValue(jsonString, Select.class);

    assertEquals(select, readCondition);
  }
}
