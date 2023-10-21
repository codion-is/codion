/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.json.db;

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
    assertEquals(select.orderBy().orElse(null).orderByColumns(), readCondition.orderBy().get().orderByColumns());
    assertEquals(select.limit(), readCondition.limit());
    assertEquals(select.offset(), readCondition.offset());
    assertEquals(select.fetchDepth().orElse(null), readCondition.fetchDepth().orElse(null));
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
    assertEquals(update.columnValues(), readCondition.columnValues());
  }
}
