/*
 * Copyright (c) 2016 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.db.TestDomain.Department;
import is.codion.framework.db.TestDomain.Employee;
import is.codion.framework.db.criteria.Criteria;
import is.codion.framework.domain.entity.OrderBy;

import org.junit.jupiter.api.Test;

import static is.codion.framework.db.criteria.Criteria.column;
import static org.junit.jupiter.api.Assertions.*;

public final class ConditionTest {

  @Test
  void selectCondition() {
    SelectCondition condition = SelectCondition.where(column(Department.LOCATION).equalTo("New York"))
            .orderBy(OrderBy.ascending(Department.NAME))
            .build();
    assertEquals(-1, condition.limit());

    condition = SelectCondition.all(Department.TYPE)
            .limit(10)
            .build();
    assertEquals(10, condition.limit());
  }

  @Test
  void updateConditionDuplicate() {
    assertThrows(IllegalArgumentException.class, () -> UpdateCondition.all(Employee.TYPE)
            .set(Employee.COMMISSION, 123d)
            .set(Employee.COMMISSION, 123d));
  }

  @Test
  void equals() {
    Criteria criteria1 = column(Employee.NAME).in("Luke", "John");
    Criteria criteria2 = column(Employee.NAME).in("Luke", "John");
    assertEquals(SelectCondition.where(criteria1).build(), SelectCondition.where(criteria2).build());
    assertEquals(SelectCondition.where(criteria1)
                    .orderBy(OrderBy.ascending(Employee.NAME))
                    .build(),
            SelectCondition.where(criteria2)
                    .orderBy(OrderBy.ascending(Employee.NAME))
                    .build());
    assertNotEquals(SelectCondition.where(criteria1)
                    .orderBy(OrderBy.ascending(Employee.NAME))
                    .build(),
            SelectCondition.where(criteria2)
                    .build());

    assertEquals(SelectCondition.where(criteria1)
                    .attributes(Employee.NAME)
                    .build(),
            SelectCondition.where(criteria2)
                    .attributes(Employee.NAME)
                    .build());

    assertEquals(SelectCondition.where(criteria1)
                    .attributes(Employee.NAME)
                    .offset(10)
                    .build(),
            SelectCondition.where(criteria2)
                    .attributes(Employee.NAME)
                    .offset(10)
                    .build());

    assertNotEquals(SelectCondition.where(criteria1)
                    .attributes(Employee.NAME)
                    .build(),
            SelectCondition.where(criteria2)
                    .attributes(Employee.NAME)
                    .offset(10)
                    .build());

    assertNotEquals(SelectCondition.where(criteria1)
                    .attributes(Employee.NAME)
                    .build(),
            SelectCondition.where(criteria2)
                    .attributes(Employee.ID)
                    .build());
  }
}
