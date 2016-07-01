/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.remote;

import org.jminor.common.User;
import org.jminor.common.i18n.Messages;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.server.DefaultEntityConnectionServerTest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

/**
 * User: Bjorn Darri
 * Date: 1.4.2010
 * Time: 22:44:24
 */
public class RemoteEntityConnectionProviderTest {

  @BeforeClass
  public static void setUp() throws Exception {
    DefaultEntityConnectionServerTest.setUp();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    DefaultEntityConnectionServerTest.tearDown();
  }

  @Test
  public void test() throws Exception {
    final RemoteEntityConnectionProvider provider = new RemoteEntityConnectionProvider("localhost",
            User.UNIT_TEST_USER, UUID.randomUUID(), "TestClient");

    assertEquals(EntityConnection.Type.REMOTE, provider.getConnectionType());
    assertEquals(EntityConnection.Type.REMOTE, provider.getConnection().getType());

    assertEquals(Configuration.getStringValue(Configuration.SERVER_HOST_NAME), provider.getServerHostName());

    final EntityConnection db = provider.getConnection();
    assertNotNull(db);
    assertTrue(db.isConnected());
    provider.disconnect();

    final EntityConnection db2 = provider.getConnection();
    assertNotNull(db2);
    assertFalse(db == db2);
    assertTrue(db2.isConnected());
    provider.disconnect();

    EntityConnection db3 = provider.getConnection();
    assertTrue(db3.isConnected());
    DefaultEntityConnectionServerTest.getServerAdmin().disconnect(provider.getClientID());
    assertFalse(db3.isConnected());

    db3 = provider.getConnection();
    assertTrue(db3.isConnected());
    db3.disconnect();

    provider.disconnect();
    assertEquals("localhost" + " - " + Messages.get(Messages.NOT_CONNECTED), provider.getDescription());
    db3 = provider.getConnection();
    assertEquals(DefaultEntityConnectionServerTest.getServerAdmin().getServerInfo().getServerName() + "@localhost", provider.getDescription());
    db3.disconnect();
  }
}
