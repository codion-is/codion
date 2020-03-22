/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.entity;

import org.jminor.framework.domain.TestDomain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public final class DefaultOrderByTest {

  @Test
  public void samePropertyTwice() {
    assertThrows(IllegalArgumentException.class, () -> new DefaultOrderBy().ascending(TestDomain.DEPARTMENT_LOCATION)
            .descending(TestDomain.DEPARTMENT_LOCATION));
  }
}
