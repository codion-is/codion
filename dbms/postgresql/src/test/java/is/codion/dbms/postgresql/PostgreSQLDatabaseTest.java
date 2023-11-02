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
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.dbms.postgresql;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PostgreSQLDatabaseTest {

  private static final String URL = "jdbc:postgresql://host:1234/sid";

  @Test
  void name() {
    PostgreSQLDatabase database = new PostgreSQLDatabase("jdbc:postgresql://host.db:1234/sid");
    assertEquals("sid", database.name());
    database = new PostgreSQLDatabase("jdbc:postgresql://host.db:1234/sid;options");
    assertEquals("sid", database.name());
    database = new PostgreSQLDatabase("jdbc:postgresql://host.db:1234/sid?parameters");
    assertEquals("sid", database.name());
    database = new PostgreSQLDatabase("jdbc:postgresql://host.db:1234/sid?parameters;options");
    assertEquals("sid", database.name());
  }

  @Test
  void sequenceQueryNullSequence() {
    assertThrows(NullPointerException.class, () -> new PostgreSQLDatabase(URL).sequenceQuery(null));
  }

  @Test
  void autoIncrementQuery() {
    PostgreSQLDatabase db = new PostgreSQLDatabase(URL);
    assertEquals("select currval('seq')", db.autoIncrementQuery("seq"));
  }

  @Test
  void sequenceQuery() {
    PostgreSQLDatabase db = new PostgreSQLDatabase(URL);
    assertEquals("select nextval('seq')", db.sequenceQuery("seq"));
  }

  @Test
  void constructorNullHost() {
    assertThrows(NullPointerException.class, () -> new PostgreSQLDatabase(null));
  }
}