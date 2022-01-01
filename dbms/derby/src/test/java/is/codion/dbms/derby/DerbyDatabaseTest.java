/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.derby;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DerbyDatabaseTest {

  private static final String URL = "jdbc:derby://host:1234/sid";

  @Test
  void getName() {
    DerbyDatabase database = new DerbyDatabase("jdbc:derby:C:/data/sample;option=true;option2=false");
    assertEquals("C:/data/sample", database.getName());
    database = new DerbyDatabase("jdbc:derby://sample.db:1234;option=true;option2=false");
    assertEquals("sample.db:1234", database.getName());
    database = new DerbyDatabase("jdbc:derby://sample.db:1234");
    assertEquals("sample.db:1234", database.getName());
    database = new DerbyDatabase("jdbc:derby://sample.db:1234/dbname");
    assertEquals("dbname", database.getName());
  }

  @Test
  void getSequenceQuery() {
    assertThrows(UnsupportedOperationException.class, () -> new DerbyDatabase(URL).getSequenceQuery("seq"));
  }

  @Test
  void supportsIsValid() {
    final DerbyDatabase db = new DerbyDatabase(URL);
    assertTrue(db.supportsIsValid());
  }

  @Test
  void supportsNoWait() {
    final DerbyDatabase db = new DerbyDatabase(URL);
    assertEquals("for update", db.getSelectForUpdateClause());
  }

  @Test
  void getAutoIncrementQuery() {
    final DerbyDatabase db = new DerbyDatabase(URL);
    final String idSource = "id_source";
    assertEquals(DerbyDatabase.AUTO_INCREMENT_QUERY + idSource, db.getAutoIncrementQuery(idSource));
  }

  @Test
  void constructorNullUrl() {
    assertThrows(NullPointerException.class, () -> new DerbyDatabase(null));
  }
}
