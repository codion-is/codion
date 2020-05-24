/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.mariadb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MariaDBDatabaseTest {

  private static final String URL = "jdbc:mariadb://host:1234/sid";

  @Test
  public void getName() {
    MariaDBDatabase database = new MariaDBDatabase("jdbc:mariadb://host.com:1234/dbname");
    assertEquals("dbname", database.getName());
    database = new MariaDBDatabase("jdbc:mariadb://host.com:1234/dbname;option=true;option2=false");
    assertEquals("dbname", database.getName());
  }

  @Test
  public void getSequenceQuery() {
    assertThrows(UnsupportedOperationException.class, () -> new MariaDBDatabase(URL).getSequenceQuery("seq"));
  }

  @Test
  public void supportsIsValid() {
    final MariaDBDatabase db = new MariaDBDatabase(URL);
    assertTrue(db.supportsIsValid());
  }

  @Test
  public void getAutoIncrementQuery() {
    final MariaDBDatabase db = new MariaDBDatabase(URL);
    assertEquals(MariaDBDatabase.AUTO_INCREMENT_QUERY, db.getAutoIncrementQuery(null));
  }

  @Test
  public void constructorNullUrl() {
    assertThrows(NullPointerException.class, () -> new MariaDBDatabase(null));
  }
}