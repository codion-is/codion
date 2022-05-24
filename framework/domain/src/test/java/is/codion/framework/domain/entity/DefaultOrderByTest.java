/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.TestDomain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public final class DefaultOrderByTest {

  @Test
  void sameAttributeTwice() {
    assertThrows(IllegalArgumentException.class, () -> new DefaultOrderBy().ascending(TestDomain.Department.LOCATION)
            .descending(TestDomain.Department.LOCATION));
  }
}
