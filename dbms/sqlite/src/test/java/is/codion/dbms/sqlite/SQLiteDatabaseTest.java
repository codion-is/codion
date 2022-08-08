/*
 * Copyright (c) 2018 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
