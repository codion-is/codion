/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.remote;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.User;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.server.EntityConnectionServerTest;

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
    EntityConnectionServerTest.setUp();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    EntityConnectionServerTest.tearDown();
  }

  @Test
  public void test() throws Exception {
    final RemoteEntityConnectionProvider provider = new RemoteEntityConnectionProvider(User.UNIT_TEST_USER, UUID.randomUUID(), "TestClient");

    assertEquals(Configuration.getStringValue(Configuration.SERVER_HOST_NAME), provider.getServerHostName());

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

    EntityConnection db3 = provider.getConnection();
    assertTrue(db3.isConnected());
    EntityConnectionServerTest.getServerAdmin().disconnect(provider.getClientID());
    assertFalse(db3.isConnected());

    db3 = provider.getConnection();
    assertTrue(db3.isConnected());
    db3.disconnect();

    provider.disconnect();
    assertEquals("localhost" + " - " + Messages.get(Messages.NOT_CONNECTED), provider.getDescription());
    db3 = provider.getConnection();
    assertEquals(EntityConnectionServerTest.getServerAdmin().getServerInfo().getServerName() + "@localhost", provider.getDescription());
    db3.disconnect();
  }
}
