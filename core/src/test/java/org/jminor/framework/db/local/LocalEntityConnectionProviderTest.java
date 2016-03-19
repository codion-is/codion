/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.local;

import org.jminor.common.db.Database;
import org.jminor.common.db.DatabasesTest;
import org.jminor.common.model.User;
import org.jminor.framework.db.EntityConnection;

import org.junit.Test;

import static org.junit.Assert.*;

public class LocalEntityConnectionProviderTest {

  @Test
  public void test() {
    final Database database = DatabasesTest.createTestDatabaseInstance();
    final LocalEntityConnectionProvider provider = new LocalEntityConnectionProvider(User.UNIT_TEST_USER, database);

    assertEquals(database.getHost(), provider.getServerHostName());

    final EntityConnection firstConnection = provider.getConnection();
    assertEquals(EntityConnection.Type.LOCAL, firstConnection.getType());
    assertNotNull(firstConnection);
    assertTrue(firstConnection.isConnected());
    provider.disconnect();

    final EntityConnection secondConnection = provider.getConnection();
    assertEquals(EntityConnection.Type.LOCAL, secondConnection.getType());
    assertNotNull(secondConnection);
    assertFalse(firstConnection == secondConnection);
    assertTrue(secondConnection.isConnected());
    provider.disconnect();
  }
}
