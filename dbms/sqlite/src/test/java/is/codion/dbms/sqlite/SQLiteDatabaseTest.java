/*
 * Copyright (c) 2018 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.sqlite;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SQLiteDatabaseTest {

@Test
  void getName() {
    SQLiteDatabase database = new SQLiteDatabase("jdbc:sqlite:/path/to/file.db");
    assertEquals("/path/to/file.db", database.getName());
    database = new SQLiteDatabase("jdbc:sqlite:/path/to/file.db;options");
    assertEquals("/path/to/file.db", database.getName());
  }

  @Test
  void getAutoIncrementQuery() {
    SQLiteDatabase database = new SQLiteDatabase("test");
    assertEquals("select last_insert_rowid()", database.getAutoIncrementQuery(null));
  }
}
