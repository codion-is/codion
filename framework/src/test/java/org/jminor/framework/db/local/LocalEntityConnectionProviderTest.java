/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.local;

import org.jminor.common.User;
import org.jminor.common.db.Database;
import org.jminor.common.db.Databases;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.EntityConnectionProviders;

import org.junit.Test;

import static org.junit.Assert.*;

public class LocalEntityConnectionProviderTest {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger"));

  @Test
  public void test() {
    final Database database = Databases.createInstance();
    final LocalEntityConnectionProvider provider = new LocalEntityConnectionProvider(UNIT_TEST_USER, database);

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

  @Test
  public void entityConnectionProviders() {
    final Object previousValue = Configuration.getValue(Configuration.CLIENT_CONNECTION_TYPE);
    Configuration.setValue(Configuration.CLIENT_CONNECTION_TYPE, Configuration.CONNECTION_TYPE_LOCAL);
    final EntityConnectionProvider connectionProvider = EntityConnectionProviders.connectionProvider(UNIT_TEST_USER, "test");
    assertEquals("LocalEntityConnectionProvider", connectionProvider.getClass().getSimpleName());
    assertEquals(EntityConnection.Type.LOCAL, connectionProvider.getConnectionType());
    Configuration.setValue(Configuration.CLIENT_CONNECTION_TYPE, previousValue);
  }
}
