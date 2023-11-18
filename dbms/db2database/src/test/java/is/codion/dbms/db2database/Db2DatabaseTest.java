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
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.dbms.db2database;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class Db2DatabaseTest {

  private static final String URL = "jdbc:db2://server:6789/database";

  @Test
  void name() {
    Db2Database database = new Db2Database(URL);
    assertEquals("database", database.name());
    database = new Db2Database(URL + ";options");
    assertEquals("database", database.name());
  }

  @Test
  void autoIncrementQuery() {
    Db2Database database = new Db2Database("test");
    assertEquals("SELECT PREVIOUS VALUE FOR seq", database.autoIncrementQuery("seq"));
  }

  @Test
  void sequenceSQLNullSequence() {
    assertThrows(NullPointerException.class, () -> new Db2Database(URL).sequenceQuery(null));
  }

  @Test
  void autoIncrementQueryNullIdSource() {
    assertThrows(NullPointerException.class, () -> new Db2Database(URL).autoIncrementQuery(null));
  }

  @Test
  void constructorNullUrl() {
    assertThrows(NullPointerException.class, () -> new Db2Database(null));
  }
}
