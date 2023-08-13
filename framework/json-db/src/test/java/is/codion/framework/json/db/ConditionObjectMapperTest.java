/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.condition.SelectCondition;
import is.codion.framework.db.condition.UpdateCondition;
import is.codion.framework.db.criteria.Criteria;
import is.codion.framework.db.criteria.CustomCriteria;
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

import static is.codion.framework.db.criteria.Criteria.*;
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

    SelectCondition condition = SelectCondition.where(and(
                    foreignKey(Employee.DEPARTMENT_FK).notIn(dept1, dept2),
                    column(Employee.NAME).equalToIgnoreCase("Loc"),
                    column(Employee.EMPNO).between(10, 40),
                    column(Employee.COMMISSION).isNotNull()))
            .build();

    String jsonString = mapper.writeValueAsString(condition);
    SelectCondition readCondition = mapper.readValue(jsonString, SelectCondition.class);

    assertEquals(condition, readCondition);
    assertEquals("(deptno not in (?, ?) and upper(ename) = upper(?) and (empno >= ? and empno <= ?) and comm is not null)",
            condition.criteria().toString(entities.definition(Employee.TYPE)));
  }

  @Test
  void nullCondition() throws JsonProcessingException {
    Criteria criteria = column(Employee.COMMISSION).isNotNull();

    String jsonString = mapper.writeValueAsString(criteria);
    Criteria readCriteria = mapper.readValue(jsonString, Criteria.class);

    assertEquals(criteria.entityType(), readCriteria.entityType());
    assertEquals(criteria.columns(), readCriteria.columns());
    assertEquals(criteria.values(), readCriteria.values());
  }

  @Test
  void customCondition() throws JsonProcessingException {
    CustomCriteria customedCriteria = customCriteria(TestEntity.CRITERIA_TYPE,
            asList(TestEntity.DECIMAL, TestEntity.DATE_TIME),
            asList(BigDecimal.valueOf(123.4), LocalDateTime.now()));
    SelectCondition condition = SelectCondition.where(customedCriteria).build();

    String jsonString = mapper.writeValueAsString(condition);
    SelectCondition readCondition = mapper.readValue(jsonString, SelectCondition.class);
    CustomCriteria readCriteria = (CustomCriteria) readCondition.criteria();

    assertEquals(customedCriteria.criteriaType(), readCriteria.criteriaType());
    assertEquals(customedCriteria.columns(), readCriteria.columns());
    assertEquals(customedCriteria.values(), readCriteria.values());
  }

  @Test
  void selectCondition() throws JsonProcessingException {
    SelectCondition selectCondition = SelectCondition.where(column(Employee.EMPNO).equalTo(1))
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

    String jsonString = mapper.writeValueAsString(selectCondition);
    SelectCondition readCondition = mapper.readValue(jsonString, SelectCondition.class);

    assertEquals(selectCondition.criteria(), readCondition.criteria());
    assertEquals(selectCondition.orderBy().orElse(null).orderByColumns(), readCondition.orderBy().get().orderByColumns());
    assertEquals(selectCondition.limit(), readCondition.limit());
    assertEquals(selectCondition.offset(), readCondition.offset());
    assertEquals(selectCondition.fetchDepth().orElse(null), readCondition.fetchDepth().orElse(null));
    for (ForeignKey foreignKey : entities.definition(selectCondition.criteria().entityType()).foreignKeys()) {
      assertEquals(selectCondition.fetchDepth(foreignKey), readCondition.fetchDepth(foreignKey));
    }
    assertEquals(selectCondition.attributes(), readCondition.attributes());
    assertTrue(readCondition.forUpdate());
    assertEquals(42, readCondition.queryTimeout());
    assertEquals(selectCondition, readCondition);

    selectCondition = SelectCondition.where(column(Employee.EMPNO).equalTo(1)).build();

    jsonString = mapper.writeValueAsString(selectCondition);
    readCondition = mapper.readValue(jsonString, SelectCondition.class);

    assertFalse(readCondition.orderBy().isPresent());
    assertFalse(readCondition.fetchDepth().isPresent());

    SelectCondition condition = SelectCondition.where(column(Employee.EMPNO).equalTo(2)).build();
    jsonString = mapper.writeValueAsString(condition);

    selectCondition = mapper.readValue(jsonString, SelectCondition.class);
  }

  @Test
  void updateCondition() throws JsonProcessingException {
    UpdateCondition condition = UpdateCondition.where(column(Department.DEPTNO)
                    .between(1, 2))
            .set(Department.LOCATION, "loc")
            .set(Department.DEPTNO, 3)
            .build();

    String jsonString = mapper.writeValueAsString(condition);
    UpdateCondition readCondition = mapper.readValue(jsonString, UpdateCondition.class);

    assertEquals(condition.criteria(), readCondition.criteria());
    assertEquals(condition.columnValues(), readCondition.columnValues());
  }

  @Test
  void allCondition() throws JsonProcessingException {
    SelectCondition condition = SelectCondition.all(Department.TYPE).build();

    String jsonString = mapper.writeValueAsString(condition);
    SelectCondition readCondition = mapper.readValue(jsonString, SelectCondition.class);

    assertEquals(condition, readCondition);
  }

  @Test
  void combinationOfCombinations() throws JsonProcessingException {
    SelectCondition condition = SelectCondition.where(and(
                    column(Employee.COMMISSION).equalTo(100d),
                    or(column(Employee.JOB).notEqualTo("test"),
                            column(Employee.JOB).isNotNull())))
            .build();

    String jsonString = mapper.writeValueAsString(condition);
    SelectCondition readCondition = mapper.readValue(jsonString, SelectCondition.class);

    assertEquals(condition, readCondition);
  }
}
