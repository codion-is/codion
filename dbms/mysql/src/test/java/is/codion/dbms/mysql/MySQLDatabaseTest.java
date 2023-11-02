/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.mysql;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MySQLDatabaseTest {

  private static final String URL = "jdbc:mysql://host:1234/sid";

  @Test
  void name() {
    MySQLDatabase database = new MySQLDatabase("jdbc:mysql://host.com:1234/dbname");
    assertEquals("dbname", database.name());
    database = new MySQLDatabase("jdbc:mysql://host.com:1234/dbname;option=true;option2=false");
    assertEquals("dbname", database.name());
  }

  @Test
  void sequenceQuery() {
    assertThrows(UnsupportedOperationException.class, () -> new MySQLDatabase(URL).sequenceQuery("seq"));
  }

  @Test
  void autoIncrementQuery() {
    MySQLDatabase db = new MySQLDatabase(URL);
    assertEquals(MySQLDatabase.AUTO_INCREMENT_QUERY, db.autoIncrementQuery(null));
  }

  @Test
  void constructorNullUrl() {
    assertThrows(NullPointerException.class, () -> new MySQLDatabase(null));
  }
}