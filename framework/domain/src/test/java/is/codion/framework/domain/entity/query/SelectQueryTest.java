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
    assertThrows(IllegalStateException.class, () -> SelectQuery.builder()
            .query("select * from dual")
            .fromClause("dual")
            .build());
    assertThrows(IllegalStateException.class, () -> SelectQuery.builder()
            .query("select * from dual")
            .whereClause("1 = 1")
            .build());
    assertThrows(IllegalStateException.class, () -> SelectQuery.builder()
            .fromClause("dual")
            .query("select * from dual")
            .build());
    assertThrows(IllegalStateException.class, () -> SelectQuery.builder()
            .whereClause("1 = 1")
            .query("select * from dual")
            .build());

    SelectQuery selectQuery = SelectQuery.builder()
            .query("select 1 from dual")
            .build();
    assertEquals("select 1 from dual", selectQuery.getQuery());
    assertTrue(selectQuery.containsColumnsClause());
    assertFalse(selectQuery.containsWhereClause());

    selectQuery = SelectQuery.builder()
            .queryContainingWhereClause("select 1 from dual where 1 = 1")
            .build();
    assertEquals("select 1 from dual where 1 = 1", selectQuery.getQuery());
    assertTrue(selectQuery.containsColumnsClause());
    assertTrue(selectQuery.containsWhereClause());

    selectQuery = SelectQuery.builder()
            .fromClause("dual")
            .build();
    assertEquals("from dual", selectQuery.getQuery());
    assertFalse(selectQuery.containsColumnsClause());
    assertFalse(selectQuery.containsWhereClause());

    selectQuery = SelectQuery.builder()
            .fromClause("dual")
            .whereClause("1 = 1")
            .build();
    assertEquals("from dual\nwhere 1 = 1", selectQuery.getQuery());
    assertFalse(selectQuery.containsColumnsClause());
    assertTrue(selectQuery.containsWhereClause());

    assertThrows(IllegalStateException.class, () -> SelectQuery.builder().fromClause("dual").query("select 1 from dual"));
    assertThrows(IllegalStateException.class, () -> SelectQuery.builder().fromClause("dual").queryContainingWhereClause("select 1 from dual"));
    assertThrows(IllegalStateException.class, () -> SelectQuery.builder().query("select 1 from dual").fromClause("dual"));
    assertThrows(IllegalStateException.class, () -> SelectQuery.builder().query("select 1 from dual").whereClause("dual"));
    assertThrows(IllegalStateException.class, () -> SelectQuery.builder().queryContainingWhereClause("select 1 from dual where 1 = 1").fromClause("dual"));
    assertThrows(IllegalStateException.class, () -> SelectQuery.builder().queryContainingWhereClause("select 1 from dual where 1 = 1").whereClause("dual"));
    assertThrows(IllegalStateException.class, () -> SelectQuery.builder().build());
    assertThrows(IllegalArgumentException.class, () -> SelectQuery.builder().whereClause("where 1 = 1"));
    assertThrows(IllegalArgumentException.class, () -> SelectQuery.builder().fromClause("From dual"));
  }
}
