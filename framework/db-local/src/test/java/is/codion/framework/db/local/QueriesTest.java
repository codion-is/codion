/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.OrderBy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class QueriesTest {

  @Test
  public void getOrderByClause() {
    final TestDomain domain = new TestDomain();
    final EntityDefinition definition = domain.getDefinition(TestDomain.Department.TYPE);
    OrderBy orderBy = OrderBy.orderBy().ascending(TestDomain.Department.LOC)
            .descending(TestDomain.Department.DNAME);
    assertEquals("order by loc, dname desc", Queries.getOrderByClause(orderBy, definition));
    orderBy = OrderBy.orderBy().ascending(TestDomain.Department.LOC)
            .descending(TestDomain.Department.DNAME).ascending(TestDomain.Department.DEPTNO);
    assertEquals("order by loc, dname desc, deptno", Queries.getOrderByClause(orderBy, definition));

    final OrderBy emptyOrderBy = OrderBy.orderBy();
    assertThrows(IllegalArgumentException.class, () -> Queries.getOrderByClause(emptyOrderBy, definition));
  }
}
