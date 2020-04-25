/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.derby;

import org.jminor.common.db.database.Database;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class DerbyDatabaseTest {

  @Test
  public void getSequenceQuery() {
    assertThrows(UnsupportedOperationException.class, () -> DerbyDatabase.derbyServerDatabase("host", 1234, "sid").getSequenceQuery("seq"));
  }

  @Test
  public void supportsIsValid() {
    final DerbyDatabase db = DerbyDatabase.derbyServerDatabase("host", 1234, "sid");
    assertTrue(db.supportsIsValid());
  }

  @Test
  public void supportsNoWait() {
    final DerbyDatabase db = DerbyDatabase.derbyServerDatabase("host", 1234, "sid");
    assertEquals(Database.SelectForUpdateSupport.FOR_UPDATE, db.getSelectForUpdateSupport());
  }

  @Test
  public void getAuthenticationInfo() {
    final DerbyDatabase db = DerbyDatabase.derbyServerDatabase("host", 1234, "sid");
    final Properties props = new Properties();
    props.put(Database.USER_PROPERTY, "scott");
    props.put(Database.PASSWORD_PROPERTY, "tiger");
    assertEquals("user=scott;password=tiger", db.getAuthenticationInfo(props));
  }

  @Test
  public void getAutoIncrementQuery() {
    final DerbyDatabase db = DerbyDatabase.derbyServerDatabase("host", 1234, "sid");
    final String idSource = "id_source";
    assertEquals(DerbyDatabase.AUTO_INCREMENT_QUERY + idSource, db.getAutoIncrementQuery(idSource));
  }

  @Test
  public void getURL() {
    DerbyDatabase db = DerbyDatabase.derbyServerDatabase("host", 1234, "sid");
    final Properties props = new Properties();
    props.put(Database.USER_PROPERTY, "scott");
    assertEquals("jdbc:derby://host:1234/sid;user=scott", db.getURL(props));
    props.put(Database.PASSWORD_PROPERTY, "tiger");
    assertEquals("jdbc:derby://host:1234/sid;user=scott;password=tiger", db.getURL(props));

    db = DerbyDatabase.derbyFileDatabase("dbname");
    assertEquals("jdbc:derby:dbname;user=scott;password=tiger", db.getURL(props));
  }

  @Test
  public void constructorNullHost() {
    assertThrows(NullPointerException.class, () -> DerbyDatabase.derbyServerDatabase(null, null, null));
  }
}
