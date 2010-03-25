package org.jminor.common.db.dbms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Properties;

public class DerbyDatabaseTest {

  @Test
  public void test() {
    DerbyDatabase db = new DerbyDatabase("host", "1234", "sid");
    final Properties props = new Properties();
    props.put("user", "scott");
    props.put("password", "tiger");
    assertTrue(db.supportsIsValid());
    assertEquals("user=scott;password=tiger", db.getAuthenticationInfo(props));
    assertEquals("select IDENTITY_VAL_LOCAL() from id_source", db.getAutoIncrementValueSQL("id_source"));
    try {
      db.getSequenceSQL("seq");
      fail();
    }
    catch (RuntimeException e) {}
    assertEquals("jdbc:derby://host:1234/sid;user=scott;password=tiger", db.getURL(props));
    final Timestamp date = new Timestamp(System.currentTimeMillis());
    assertEquals("DATE('" + new SimpleDateFormat("yyyy-MM-dd").format(date) + "')", db.getSQLDateString(date, false));
    assertEquals("DATE('" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date) + "')", db.getSQLDateString(date, true));

    db = new DerbyDatabase("dbname");
    assertEquals("jdbc:derby:dbname;user=scott;password=tiger", db.getURL(props));
  }
}
