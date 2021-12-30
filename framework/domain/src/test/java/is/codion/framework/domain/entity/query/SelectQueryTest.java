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
    assertThrows(IllegalArgumentException.class, () -> SelectQuery.builder()
            .fromClause("dual")
            .whereClause("1 = 1")
            .orderByClause(" order BY 1")
            .build());

    SelectQuery selectQuery = SelectQuery.builder().columnsClause("1").fromClause("dual").build();
    assertEquals("1", selectQuery.getColumnsClause());
    assertEquals("dual", selectQuery.getFromClause());
    assertNull(selectQuery.getWhereClause());

    selectQuery = SelectQuery.builder().columnsClause("1").fromClause("dual").whereClause("1 = 1").build();
    assertEquals("1", selectQuery.getColumnsClause());
    assertEquals("dual", selectQuery.getFromClause());
    assertEquals("1 = 1", selectQuery.getWhereClause());
    assertNotNull(selectQuery.getWhereClause());

    selectQuery = SelectQuery.builder()
            .fromClause("dual")
            .build();
    assertEquals("dual", selectQuery.getFromClause());
    assertNull(selectQuery.getWhereClause());

    selectQuery = SelectQuery.builder()
            .fromClause("dual")
            .whereClause("1 = 1")
            .build();
    assertEquals("dual", selectQuery.getFromClause());
    assertEquals("1 = 1", selectQuery.getWhereClause());
    assertNotNull(selectQuery.getWhereClause());

    assertThrows(IllegalStateException.class, () -> SelectQuery.builder().build());
    assertThrows(IllegalArgumentException.class, () -> SelectQuery.builder().whereClause("where 1 = 1"));
    assertThrows(IllegalArgumentException.class, () -> SelectQuery.builder().fromClause("From dual"));
    assertThrows(IllegalArgumentException.class, () -> SelectQuery.builder().orderByClause("order By 1"));
  }
}
