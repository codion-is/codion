/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.util.Properties;

public class H2DatabaseTest {

  @Test
  public void test() {
    H2Database db = new H2Database("host", "1234", "sid");
    final Properties props = new Properties();
    props.put("user", "scott");
    props.put("password", "tiger");
    assertTrue(db.supportsIsValid());
    assertEquals("user=scott;password=tiger", db.getAuthenticationInfo(props));
    assertEquals("CALL IDENTITY()", db.getAutoIncrementValueSQL(null));
    assertEquals("select next value for seq", db.getSequenceSQL("seq"));
    assertEquals("jdbc:h2://host:1234/sid;user=scott;password=tiger", db.getURL(props));

    db = new H2Database("dbname");
    assertEquals("jdbc:h2:dbname;user=scott;password=tiger", db.getURL(props));
  }
}