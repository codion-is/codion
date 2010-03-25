/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import static org.junit.Assert.*;
import org.junit.Test;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class PostgreSQLDatabaseTest {

  @Test
  public void test() {
    PostgreSQLDatabase db = new PostgreSQLDatabase("host", "1234", "sid");
    assertFalse(db.supportsIsValid());
    assertNull(db.getAuthenticationInfo(null));
    assertEquals("select currval(seq)", db.getAutoIncrementValueSQL("seq"));
    assertEquals("select nextval(seq)", db.getSequenceSQL("seq"));
    assertEquals("jdbc:postgresql://host:1234/sid", db.getURL(null));
    final Timestamp date = new Timestamp(System.currentTimeMillis());
    assertEquals("to_date('" + new SimpleDateFormat("dd-MM-yyyy").format(date) + "', 'DD-MM-YYYY')", db.getSQLDateString(date, false));
    assertEquals("to_date('" + new SimpleDateFormat("dd-MM-yyyy HH:mm").format(date)  + "', 'DD-MM-YYYY HH24:MI')", db.getSQLDateString(date, true));
  }
}