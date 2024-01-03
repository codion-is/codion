/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
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
    assertEquals("1", selectQuery.columns());
    assertEquals("dual", selectQuery.from());
    assertNull(selectQuery.where());

    selectQuery = SelectQuery.builder()
            .columns("1")
            .from("dual")
            .where("1 = 1")
            .build();
    assertEquals("1", selectQuery.columns());
    assertEquals("dual", selectQuery.from());
    assertEquals("1 = 1", selectQuery.where());
    assertNotNull(selectQuery.where());

    selectQuery = SelectQuery.builder()
            .from("dual")
            .build();
    assertEquals("dual", selectQuery.from());
    assertNull(selectQuery.where());

    selectQuery = SelectQuery.builder()
            .from("dual")
            .where("1 = 1")
            .build();
    assertEquals("dual", selectQuery.from());
    assertEquals("1 = 1", selectQuery.where());
    assertNotNull(selectQuery.where());

    assertThrows(NullPointerException.class, () -> SelectQuery.builder().from(null).build());
    assertThrows(IllegalArgumentException.class, () -> SelectQuery.builder().from("dual").where("where 1 = 1"));
    assertThrows(IllegalArgumentException.class, () -> SelectQuery.builder().from("From dual"));
    assertThrows(IllegalArgumentException.class, () -> SelectQuery.builder().from("dual").orderBy("order By 1"));
  }
}
