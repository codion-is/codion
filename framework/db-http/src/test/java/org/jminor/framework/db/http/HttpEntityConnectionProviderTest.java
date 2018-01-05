/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.http;

import org.jminor.common.User;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.EntityConnectionProviders;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HttpEntityConnectionProviderTest {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger"));

  @Test
  public void entityConnectionProviders() {
    final String previousValue = EntityConnectionProvider.CLIENT_CONNECTION_TYPE.get();
    EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(EntityConnectionProvider.CONNECTION_TYPE_HTTP);
    HttpEntityConnectionProvider.HTTP_SERVER_PORT.set(8089);
    final EntityConnectionProvider connectionProvider = EntityConnectionProviders.connectionProvider(TestDomain.class.getName(), UNIT_TEST_USER, "test");
    assertEquals("HttpEntityConnectionProvider", connectionProvider.getClass().getSimpleName());
    assertEquals(EntityConnection.Type.HTTP, connectionProvider.getConnectionType());
    EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(previousValue);
  }
}
