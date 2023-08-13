/*
 * Copyright (c) 2016 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db;

import is.codion.framework.db.TestDomain.Department;
import is.codion.framework.db.TestDomain.Employee;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.domain.entity.OrderBy;

import org.junit.jupiter.api.Test;

import static is.codion.framework.db.condition.Condition.column;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public final class SelectTest {

  @Test
  void selectCondition() {
    Select select = Select.where(column(Department.LOCATION).equalTo("New York"))
            .orderBy(OrderBy.ascending(Department.NAME))
            .build();
    assertEquals(-1, select.limit());

    select = Select.all(Department.TYPE)
            .limit(10)
            .build();
    assertEquals(10, select.limit());
  }

  @Test
  void equals() {
    Condition condition1 = column(Employee.NAME).in("Luke", "John");
    Condition condition2 = column(Employee.NAME).in("Luke", "John");
    assertEquals(Select.where(condition1).build(), Select.where(condition2).build());
    assertEquals(Select.where(condition1)
                    .orderBy(OrderBy.ascending(Employee.NAME))
                    .build(),
            Select.where(condition2)
                    .orderBy(OrderBy.ascending(Employee.NAME))
                    .build());
    assertNotEquals(Select.where(condition1)
                    .orderBy(OrderBy.ascending(Employee.NAME))
                    .build(),
            Select.where(condition2)
                    .build());

    assertEquals(Select.where(condition1)
                    .attributes(Employee.NAME)
                    .build(),
            Select.where(condition2)
                    .attributes(Employee.NAME)
                    .build());

    assertEquals(Select.where(condition1)
                    .attributes(Employee.NAME)
                    .offset(10)
                    .build(),
            Select.where(condition2)
                    .attributes(Employee.NAME)
                    .offset(10)
                    .build());

    assertNotEquals(Select.where(condition1)
                    .attributes(Employee.NAME)
                    .build(),
            Select.where(condition2)
                    .attributes(Employee.NAME)
                    .offset(10)
                    .build());

    assertNotEquals(Select.where(condition1)
                    .attributes(Employee.NAME)
                    .build(),
            Select.where(condition2)
                    .attributes(Employee.ID)
                    .build());
  }
}
