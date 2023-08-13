/*
 * Copyright (c) 2016 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.db.Select;
import is.codion.framework.db.TestDomain.Department;
import is.codion.framework.db.TestDomain.Employee;
import is.codion.framework.db.Update;
import is.codion.framework.db.criteria.Criteria;
import is.codion.framework.domain.entity.OrderBy;

import org.junit.jupiter.api.Test;

import static is.codion.framework.db.criteria.Criteria.column;
import static org.junit.jupiter.api.Assertions.*;

public final class ConditionTest {

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
  void updateConditionDuplicate() {
    assertThrows(IllegalArgumentException.class, () -> Update.all(Employee.TYPE)
            .set(Employee.COMMISSION, 123d)
            .set(Employee.COMMISSION, 123d));
  }

  @Test
  void equals() {
    Criteria criteria1 = column(Employee.NAME).in("Luke", "John");
    Criteria criteria2 = column(Employee.NAME).in("Luke", "John");
    assertEquals(Select.where(criteria1).build(), Select.where(criteria2).build());
    assertEquals(Select.where(criteria1)
                    .orderBy(OrderBy.ascending(Employee.NAME))
                    .build(),
            Select.where(criteria2)
                    .orderBy(OrderBy.ascending(Employee.NAME))
                    .build());
    assertNotEquals(Select.where(criteria1)
                    .orderBy(OrderBy.ascending(Employee.NAME))
                    .build(),
            Select.where(criteria2)
                    .build());

    assertEquals(Select.where(criteria1)
                    .attributes(Employee.NAME)
                    .build(),
            Select.where(criteria2)
                    .attributes(Employee.NAME)
                    .build());

    assertEquals(Select.where(criteria1)
                    .attributes(Employee.NAME)
                    .offset(10)
                    .build(),
            Select.where(criteria2)
                    .attributes(Employee.NAME)
                    .offset(10)
                    .build());

    assertNotEquals(Select.where(criteria1)
                    .attributes(Employee.NAME)
                    .build(),
            Select.where(criteria2)
                    .attributes(Employee.NAME)
                    .offset(10)
                    .build());

    assertNotEquals(Select.where(criteria1)
                    .attributes(Employee.NAME)
                    .build(),
            Select.where(criteria2)
                    .attributes(Employee.ID)
                    .build());
  }
}
