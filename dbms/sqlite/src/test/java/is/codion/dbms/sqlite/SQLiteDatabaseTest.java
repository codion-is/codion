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
 * Copyright (c) 2018 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.dbms.sqlite;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SQLiteDatabaseTest {

  @Test
  void name() {
    SQLiteDatabase database = new SQLiteDatabase("jdbc:sqlite:/path/to/file.db");
    assertEquals("/path/to/file.db", database.name());
    database = new SQLiteDatabase("jdbc:sqlite:/path/to/file.db;options");
    assertEquals("/path/to/file.db", database.name());
  }

  @Test
  void autoIncrementQuery() {
    SQLiteDatabase database = new SQLiteDatabase("test");
    assertEquals("select last_insert_rowid()", database.autoIncrementQuery(null));
  }
}
