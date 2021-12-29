/*
 * Copyright (c) 2021 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SelectQueryTest {

  @Test
  void test() {
    assertThrows(IllegalArgumentException.class, () -> SelectQuery.builder()
            .fromClause("from dual")
            .build());
    assertThrows(IllegalArgumentException.class, () -> SelectQuery.builder()
            .fromClause("dual")
            .whereClause("WHERE 1 = 1")
            .build());
    assertThrows(IllegalArgumentException.class, () -> SelectQuery.builder()
            .fromClause("\n from dual")
            .build());
    assertThrows(IllegalArgumentException.class, () -> SelectQuery.builder()
            .fromClause("dual")
            .whereClause("   wheRE 1 = 1")
            .build());

    SelectQuery selectQuery = SelectQuery.query("select 1 from dual");
    assertEquals("select 1 from dual", selectQuery.getQuery());
    assertFalse(selectQuery.containsWhereClause());

    selectQuery = SelectQuery.queryContainingWhereClause("select 1 from dual where 1 = 1");
    assertEquals("select 1 from dual where 1 = 1", selectQuery.getQuery());
    assertTrue(selectQuery.containsWhereClause());

    selectQuery = SelectQuery.builder()
            .fromClause("dual")
            .build();
    assertEquals("dual", selectQuery.getFromClause());
    assertFalse(selectQuery.containsWhereClause());

    selectQuery = SelectQuery.builder()
            .fromClause("dual")
            .whereClause("1 = 1")
            .build();
    assertEquals("dual", selectQuery.getFromClause());
    assertEquals("1 = 1", selectQuery.getWhereClause());
    assertTrue(selectQuery.containsWhereClause());

    assertThrows(IllegalStateException.class, () -> SelectQuery.builder().build());
    assertThrows(IllegalArgumentException.class, () -> SelectQuery.builder().whereClause("where 1 = 1"));
    assertThrows(IllegalArgumentException.class, () -> SelectQuery.builder().fromClause("From dual"));
  }
}
