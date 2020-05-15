/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.sqlite;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SQLiteDatabaseTest {

@Test
  public void getName() {
    SQLiteDatabase database = new SQLiteDatabase("jdbc:sqlite:/path/to/file.db");
    assertEquals("/path/to/file.db", database.getName());
    database = new SQLiteDatabase("jdbc:sqlite:/path/to/file.db;options");
    assertEquals("/path/to/file.db", database.getName());
  }

  @Test
  public void getAutoIncrementQuery() {
    final SQLiteDatabase database = new SQLiteDatabase("test");
    assertEquals("select last_insert_rowid()", database.getAutoIncrementQuery(null));
  }
}
