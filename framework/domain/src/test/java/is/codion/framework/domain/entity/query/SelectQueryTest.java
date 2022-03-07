/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SelectQueryTest {

  @Test
  void test() {
    assertThrows(IllegalArgumentException.class, () -> SelectQuery.builder()
            .from("from dual")
            .build());
    assertThrows(IllegalArgumentException.class, () -> SelectQuery.builder()
            .from("dual")
            .where("WHERE 1 = 1")
            .build());
    assertThrows(IllegalArgumentException.class, () -> SelectQuery.builder()
            .from("\n from dual")
            .build());
    assertThrows(IllegalArgumentException.class, () -> SelectQuery.builder()
            .from("dual")
            .where("   wheRE 1 = 1")
            .build());
    assertThrows(IllegalArgumentException.class, () -> SelectQuery.builder()
            .from("dual")
            .where("1 = 1")
            .orderBy(" order BY 1")
            .build());

    SelectQuery selectQuery = SelectQuery.builder()
            .columns("1")
            .from("dual")
            .build();
    assertEquals("1", selectQuery.getColumns());
    assertEquals("dual", selectQuery.getFrom());
    assertNull(selectQuery.getWhere());

    selectQuery = SelectQuery.builder()
            .columns("1")
            .from("dual")
            .where("1 = 1")
            .build();
    assertEquals("1", selectQuery.getColumns());
    assertEquals("dual", selectQuery.getFrom());
    assertEquals("1 = 1", selectQuery.getWhere());
    assertNotNull(selectQuery.getWhere());

    selectQuery = SelectQuery.builder()
            .from("dual")
            .build();
    assertEquals("dual", selectQuery.getFrom());
    assertNull(selectQuery.getWhere());

    selectQuery = SelectQuery.builder()
            .from("dual")
            .where("1 = 1")
            .build();
    assertEquals("dual", selectQuery.getFrom());
    assertEquals("1 = 1", selectQuery.getWhere());
    assertNotNull(selectQuery.getWhere());

    assertThrows(NullPointerException.class, () -> SelectQuery.builder().from(null).build());
    assertThrows(IllegalArgumentException.class, () -> SelectQuery.builder().from("dual").where("where 1 = 1"));
    assertThrows(IllegalArgumentException.class, () -> SelectQuery.builder().from("From dual"));
    assertThrows(IllegalArgumentException.class, () -> SelectQuery.builder().from("dual").orderBy("order By 1"));
  }
}
