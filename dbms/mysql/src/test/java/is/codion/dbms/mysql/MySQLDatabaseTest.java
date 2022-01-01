/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.mysql;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MySQLDatabaseTest {

  private static final String URL = "jdbc:mysql://host:1234/sid";

  @Test
  void getName() {
    MySQLDatabase database = new MySQLDatabase("jdbc:mysql://host.com:1234/dbname");
    assertEquals("dbname", database.getName());
    database = new MySQLDatabase("jdbc:mysql://host.com:1234/dbname;option=true;option2=false");
    assertEquals("dbname", database.getName());
  }

  @Test
  void getSequenceQuery() {
    assertThrows(UnsupportedOperationException.class, () -> new MySQLDatabase(URL).getSequenceQuery("seq"));
  }

  @Test
  void supportsIsValid() {
    final MySQLDatabase db = new MySQLDatabase(URL);
    assertTrue(db.supportsIsValid());
  }

  @Test
  void getAutoIncrementQuery() {
    final MySQLDatabase db = new MySQLDatabase(URL);
    assertEquals(MySQLDatabase.AUTO_INCREMENT_QUERY, db.getAutoIncrementQuery(null));
  }

  @Test
  void constructorNullUrl() {
    assertThrows(NullPointerException.class, () -> new MySQLDatabase(null));
  }
}