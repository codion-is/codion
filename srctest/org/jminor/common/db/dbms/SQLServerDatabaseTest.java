package org.jminor.common.db.dbms;

import static org.junit.Assert.*;
import org.junit.Test;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class SQLServerDatabaseTest {

  @Test
  public void test() {
    final SQLServerDatabase db = new SQLServerDatabase("host", "1234", "sid");
    assertTrue(db.supportsIsValid());
    assertNull(db.getAuthenticationInfo(null));
    assertEquals("SELECT SCOPE_IDENTITY()", db.getAutoIncrementValueSQL(null));
    try {
      db.getSequenceSQL("seq");
      fail();
    }
    catch (RuntimeException e) {}
    assertEquals("jdbc:sqlserver://host:1234;databaseName=sid", db.getURL(null));
    final Timestamp date = new Timestamp(System.currentTimeMillis());
    assertEquals("convert(datetime, '" + new SimpleDateFormat("MM-dd-yyyy").format(date) + "', 105)", db.getSQLDateString(date, false));
    assertEquals("convert(datetime, '" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date) + "', 120)", db.getSQLDateString(date, true));
  }
}