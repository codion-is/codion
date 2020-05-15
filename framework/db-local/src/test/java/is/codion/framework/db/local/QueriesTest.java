/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.db.local;

import dev.codion.framework.domain.entity.EntityDefinition;
import dev.codion.framework.domain.entity.OrderBy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class QueriesTest {

  @Test
  public void getOrderByClause() {
    final TestDomain domain = new TestDomain();
    final EntityDefinition definition = domain.getDefinition(TestDomain.T_DEPARTMENT);
    OrderBy orderBy = OrderBy.orderBy().ascending(TestDomain.DEPARTMENT_LOCATION)
            .descending(TestDomain.DEPARTMENT_NAME);
    assertEquals("order by loc, dname desc", Queries.getOrderByClause(orderBy, definition));
    orderBy = OrderBy.orderBy().ascending(TestDomain.DEPARTMENT_LOCATION)
            .descending(TestDomain.DEPARTMENT_NAME).ascending(TestDomain.DEPARTMENT_ID);
    assertEquals("order by loc, dname desc, deptno", Queries.getOrderByClause(orderBy, definition));

    final OrderBy emptyOrderBy = OrderBy.orderBy();
    assertThrows(IllegalArgumentException.class, () -> Queries.getOrderByClause(emptyOrderBy, definition));
  }
}
