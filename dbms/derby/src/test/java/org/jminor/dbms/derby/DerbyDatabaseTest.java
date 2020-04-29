/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.derby;

import org.jminor.common.db.database.Database;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class DerbyDatabaseTest {

  private static final String URL = "jdbc:derby://host:1234/sid";

  @Test
  public void getSequenceQuery() {
    assertThrows(UnsupportedOperationException.class, () -> new DerbyDatabase(URL).getSequenceQuery("seq"));
  }

  @Test
  public void supportsIsValid() {
    final DerbyDatabase db = new DerbyDatabase(URL);
    assertTrue(db.supportsIsValid());
  }

  @Test
  public void supportsNoWait() {
    final DerbyDatabase db = new DerbyDatabase(URL);
    assertEquals(Database.SelectForUpdateSupport.FOR_UPDATE, db.getSelectForUpdateSupport());
  }

  @Test
  public void getAuthenticationInfo() {
    final DerbyDatabase db = new DerbyDatabase(URL);
    final Properties props = new Properties();
    props.put(Database.USER_PROPERTY, "scott");
    props.put(Database.PASSWORD_PROPERTY, "tiger");
    assertEquals("user=scott;password=tiger", db.getAuthenticationInfo(props));
  }

  @Test
  public void getAutoIncrementQuery() {
    final DerbyDatabase db = new DerbyDatabase(URL);
    final String idSource = "id_source";
    assertEquals(DerbyDatabase.AUTO_INCREMENT_QUERY + idSource, db.getAutoIncrementQuery(idSource));
  }

  @Test
  public void constructorNullUrl() {
    assertThrows(NullPointerException.class, () -> new DerbyDatabase(null));
  }
}
