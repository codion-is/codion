/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.local;

import org.jminor.common.db.Database;
import org.jminor.common.db.Databases;
import org.jminor.common.model.User;
import org.jminor.framework.db.EntityConnection;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LocalEntityConnectionProviderTest {

  @Test
  public void test() {
    final Database database = Databases.createInstance();
    final LocalEntityConnectionProvider provider = new LocalEntityConnectionProvider(User.UNIT_TEST_USER, database);

    assertEquals(database.getHost(), provider.getServerHostName());

    final EntityConnection db = provider.getConnection();
    assertNotNull(db);
    assertTrue(db.isConnected());
    assertTrue(db.isValid());
    provider.disconnect();

    final EntityConnection db2 = provider.getConnection();
    assertNotNull(db2);
    assertFalse(db == db2);
    assertTrue(db2.isConnected());
    assertTrue(db2.isValid());
    provider.disconnect();
  }
}
