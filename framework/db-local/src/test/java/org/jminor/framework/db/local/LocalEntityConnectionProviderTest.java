/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.local;

import org.jminor.common.User;
import org.jminor.common.db.Database;
import org.jminor.common.db.Databases;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.EntityConnectionProviders;
import org.jminor.framework.domain.Entities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LocalEntityConnectionProviderTest {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  private static final Entities ENTITIES = new TestDomain();

  @Test
  public void test() {
    final Database database = Databases.getInstance();
    final EntityConnectionProvider provider = new LocalEntityConnectionProvider(ENTITIES, database).setUser(UNIT_TEST_USER);

    assertEquals(database.getHost(), provider.getServerHostName());

    final EntityConnection firstConnection = provider.getConnection();
    assertEquals(EntityConnection.Type.LOCAL, firstConnection.getType());
    assertNotNull(firstConnection);
    assertTrue(firstConnection.isConnected());
    provider.disconnect();

    final EntityConnection secondConnection = provider.getConnection();
    assertEquals(EntityConnection.Type.LOCAL, secondConnection.getType());
    assertNotNull(secondConnection);
    assertNotSame(firstConnection, secondConnection);
    assertTrue(secondConnection.isConnected());
    provider.disconnect();
  }

  @Test
  public void entityConnectionProviders() {
    final String previousValue = EntityConnectionProvider.CLIENT_CONNECTION_TYPE.get();
    EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(EntityConnectionProvider.CONNECTION_TYPE_LOCAL);
    final EntityConnectionProvider connectionProvider = EntityConnectionProviders.connectionProvider(TestDomain.class.getName(), "test");
    assertEquals("LocalEntityConnectionProvider", connectionProvider.getClass().getSimpleName());
    assertEquals(EntityConnection.Type.LOCAL, connectionProvider.getConnectionType());
    EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(previousValue);
  }
}
