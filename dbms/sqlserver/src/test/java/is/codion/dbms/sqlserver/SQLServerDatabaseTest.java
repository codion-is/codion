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
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.dbms.sqlserver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SQLServerDatabaseTest {

  private static final String URL = "jdbc:sqlserver://host:1234;databaseName=sid";

  @Test
  void name() {
    SQLServerDatabase database = new SQLServerDatabase("jdbc:sqlserver://host.db\\instance:1234");
    assertEquals("instance", database.name());
    database = new SQLServerDatabase("jdbc:sqlserver://host.db\\instance:1234;options");
    assertEquals("instance", database.name());
  }

  @Test
  void sequenceQuery() {
    assertThrows(UnsupportedOperationException.class, () -> new SQLServerDatabase(URL).sequenceQuery("seq"));
  }

  @Test
  void autoIncrementQuery() {
    SQLServerDatabase db = new SQLServerDatabase(URL);
    assertEquals(SQLServerDatabase.AUTO_INCREMENT_QUERY, db.autoIncrementQuery(null));
  }

  @Test
  void constructorNullHost() {
    assertThrows(NullPointerException.class, () -> new SQLServerDatabase(null));
  }
}