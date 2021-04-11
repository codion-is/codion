/*
 * Copyright (c) 2021 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class SelectQueryTest {

  @Test
  public void test() {
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
  }
}
