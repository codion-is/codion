/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class OrderByTest {

  @Test
  public void test() {
    final TestDomain domain = new TestDomain();
    final EntityDefinition definition = domain.getDefinition(TestDomain.T_DEPARTMENT);
    OrderBy orderBy = Domain.orderBy().ascending(TestDomain.DEPARTMENT_LOCATION)
            .descending(TestDomain.DEPARTMENT_NAME);
    assertEquals("loc, dname desc", orderBy.getOrderByString(definition));
    orderBy = Domain.orderBy().ascending(TestDomain.DEPARTMENT_LOCATION)
            .descending(TestDomain.DEPARTMENT_NAME).ascending(TestDomain.DEPARTMENT_ID);
    assertEquals("loc, dname desc, deptno", orderBy.getOrderByString(definition));
  }

  @Test
  public void samePropertyTwice() {
    assertThrows(IllegalArgumentException.class, () -> Domain.orderBy().ascending(TestDomain.DEPARTMENT_LOCATION)
            .descending(TestDomain.DEPARTMENT_LOCATION));
  }
}
