/*
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.json.db;

import is.codion.framework.db.EntityConnection.Count;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnection.Update;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.json.TestDomain;
import is.codion.framework.json.TestDomain.Department;
import is.codion.framework.json.TestDomain.Employee;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import static is.codion.framework.json.db.DatabaseObjectMapper.databaseObjectMapper;
import static is.codion.framework.json.domain.EntityObjectMapper.entityObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

public final class DatabaseObjectMapperTest {

  private final Entities entities = new TestDomain().entities();
  private final DatabaseObjectMapper mapper = databaseObjectMapper(entityObjectMapper(entities));

  @Test
  void select() throws JsonProcessingException {
    Select select = Select.where(Employee.EMPNO.equalTo(1))
            .having(Employee.COMMISSION.greaterThan(200d))
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

    assertEquals(select.where(), readCondition.where());
    assertEquals(select.having(), readCondition.having());
    assertEquals(select.orderBy().orElse(null).orderByColumns(), readCondition.orderBy().get().orderByColumns());
    assertEquals(select.limit(), readCondition.limit());
    assertEquals(select.offset(), readCondition.offset());
    assertEquals(select.fetchDepth().orElse(-1), readCondition.fetchDepth().orElse(-1));
    for (ForeignKey foreignKey : entities.definition(select.where().entityType()).foreignKeys().get()) {
      assertEquals(select.foreignKeyFetchDepths().get(foreignKey), readCondition.foreignKeyFetchDepths().get(foreignKey));
    }
    assertEquals(select.attributes(), readCondition.attributes());
    assertTrue(readCondition.forUpdate());
    assertEquals(42, readCondition.queryTimeout());
    assertEquals(select, readCondition);

    select = Select.where(Employee.EMPNO.equalTo(1)).build();

    jsonString = mapper.writeValueAsString(select);
    readCondition = mapper.readValue(jsonString, Select.class);

    assertFalse(readCondition.orderBy().isPresent());
    assertFalse(readCondition.fetchDepth().isPresent());

    select = Select.where(Employee.EMPNO.equalTo(2)).build();
    jsonString = mapper.writeValueAsString(select);

    select = mapper.readValue(jsonString, Select.class);
  }

  @Test
  void update() throws JsonProcessingException {
    Update update = Update.where(Department.DEPTNO
                    .between(1, 2))
            .set(Department.LOCATION, "loc")
            .set(Department.DEPTNO, 3)
            .build();

    String jsonString = mapper.writeValueAsString(update);
    Update readCondition = mapper.readValue(jsonString, Update.class);

    assertEquals(update.where(), readCondition.where());
    assertEquals(update.values(), readCondition.values());
  }

  @Test
  void count() throws JsonProcessingException {
    Count count = Count.builder(Department.DEPTNO.between(1, 2))
            .having(Department.NAME.equalTo("TEST"))
            .build();

    String jsonString = mapper.writeValueAsString(count);
    Count readCount = mapper.readValue(jsonString, Count.class);

    assertEquals(count.where(), readCount.where());
    assertEquals(count.having(), readCount.having());
  }
}
