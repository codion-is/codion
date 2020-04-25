/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.hsqldb;

import org.jminor.common.db.database.Database;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class HSQLDatabaseTest {

  @Test
  public void getSequenceSQLNullSequence() {
    assertThrows(NullPointerException.class, () -> HSQLDatabase.hsqlServerDatabase("host", 1234, "sid").getSequenceQuery(null));
  }

  @Test
  public void supportsIsValid() {
    final HSQLDatabase db = HSQLDatabase.hsqlServerDatabase("host", 1234, "sid");
    assertTrue(db.supportsIsValid());
  }

  @Test
  public void getAuthenticationInfo() {
    final HSQLDatabase db = HSQLDatabase.hsqlServerDatabase("host", 1234, "sid");
    final Properties props = new Properties();
    props.put(Database.USER_PROPERTY, "scott");
    props.put(Database.PASSWORD_PROPERTY, "tiger");
    assertEquals("user=scott;password=tiger", db.getAuthenticationInfo(props));
  }

  @Test
  public void getAutoIncrementQuery() {
    final HSQLDatabase db = HSQLDatabase.hsqlServerDatabase("host", 1234, "sid");
    assertEquals(HSQLDatabase.AUTO_INCREMENT_QUERY, db.getAutoIncrementQuery(null));
  }

  @Test
  public void getSequenceQuery() {
    final HSQLDatabase db = HSQLDatabase.hsqlServerDatabase("host", 1234, "sid");
    final String idSource = "seq";
    assertEquals(HSQLDatabase.SEQUENCE_VALUE_QUERY + idSource, db.getSequenceQuery(idSource));
  }

  @Test
  public void getURL() {
    HSQLDatabase db = HSQLDatabase.hsqlServerDatabase("host", 1234, "sid");
    final Properties props = new Properties();
    props.put(Database.USER_PROPERTY, "scott");
    assertEquals("jdbc:hsqldb:hsql//host:1234/sid;user=scott", db.getURL(props));
    props.put(Database.PASSWORD_PROPERTY, "tiger");
    assertEquals("jdbc:hsqldb:hsql//host:1234/sid;user=scott;password=tiger", db.getURL(props));
    db = HSQLDatabase.hsqlFileDatabase("dbname");
    assertEquals("jdbc:hsqldb:file:dbname;user=scott;password=tiger", db.getURL(props));
  }

  @Test
  public void constructorNullHost() {
    assertThrows(NullPointerException.class, () -> HSQLDatabase.hsqlServerDatabase(null, null, null));
  }
}