/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.TestDomain.Department;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public final class DefaultOrderByTest {

  @Test
  void sameAttributeTwice() {
    assertThrows(IllegalArgumentException.class, () -> new DefaultOrderBy.DefaultOrderByBuilder()
            .ascending(Department.LOCATION)
            .descending(Department.LOCATION));
  }

  @Test
  void noAttributes() {
    assertThrows(IllegalArgumentException.class, OrderBy::ascending);
  }

  @Test
  void equals() {
    OrderBy orderBy = OrderBy.builder()
            .ascending(Department.LOCATION)
            .descending(Department.NAME)
            .build();
    assertEquals(orderBy, orderBy);
    assertEquals(orderBy,
            OrderBy.builder()
                    .ascending(Department.LOCATION)
                    .descending(Department.NAME)
                    .build());
    assertEquals(OrderBy.builder()
                    .ascending(Department.LOCATION, Department.NAME)
                    .build(),
            OrderBy.ascending(Department.LOCATION, Department.NAME));
    assertNotEquals(OrderBy.builder()
                    .descending(Department.NAME)
                    .ascending(Department.LOCATION)
                    .build(),
            OrderBy.builder()
                    .ascending(Department.LOCATION)
                    .descending(Department.NAME)
                    .build());
  }
}
