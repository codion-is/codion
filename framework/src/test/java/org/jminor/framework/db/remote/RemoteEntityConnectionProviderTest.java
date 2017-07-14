/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.remote;

import org.jminor.common.User;
import org.jminor.common.Version;
import org.jminor.common.db.Database;
import org.jminor.common.db.Databases;
import org.jminor.common.i18n.Messages;
import org.jminor.common.server.Server;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.EntityConnectionProviders;
import org.jminor.framework.server.DefaultEntityConnectionServer;
import org.jminor.framework.server.EntityConnectionServerAdmin;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.rmi.registry.LocateRegistry;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * User: Bjorn Darri
 * Date: 1.4.2010
 * Time: 22:44:24
 */
public class RemoteEntityConnectionProviderTest {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger"));

  private static final User ADMIN_USER = new User("scott", "tiger");
  private static Server server;
  private static EntityConnectionServerAdmin admin;

  @BeforeClass
  public static synchronized void setUp() throws Exception {
    configure();
    final Database database = Databases.createInstance();
    final String serverName = Configuration.getStringValue(Configuration.SERVER_NAME_PREFIX) + " " + Version.getVersionString()
            + "@" + (database.getSid() != null ? database.getSid().toUpperCase() : database.getHost().toUpperCase());
    DefaultEntityConnectionServer.startServer();
    server = (Server) LocateRegistry.getRegistry(Configuration.getStringValue(Configuration.SERVER_HOST_NAME),
            Configuration.getIntValue(Configuration.REGISTRY_PORT)).lookup(serverName);
    admin = (EntityConnectionServerAdmin) server.getServerAdmin(ADMIN_USER);
  }

  @AfterClass
  public static synchronized void tearDown() throws Exception {
    admin.shutdown();
    server = null;
  }

  @Test
  public void test() throws Exception {
    final RemoteEntityConnectionProvider provider = new RemoteEntityConnectionProvider("localhost",
            UNIT_TEST_USER, UUID.randomUUID(), "TestClient");

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
    admin.disconnect(provider.getClientID());
    assertFalse(db3.isConnected());

    db3 = provider.getConnection();
    assertTrue(db3.isConnected());
    db3.disconnect();

    provider.disconnect();
    assertEquals("localhost" + " - " + Messages.get(Messages.NOT_CONNECTED), provider.getDescription());
    db3 = provider.getConnection();
    assertEquals(admin.getServerInfo().getServerName() + "@localhost", provider.getDescription());
    db3.disconnect();
  }

  @Test
  public void entityConnectionProviders() {
    final Object previousValue = Configuration.getValue(Configuration.CLIENT_CONNECTION_TYPE);
    Configuration.setValue(Configuration.CLIENT_CONNECTION_TYPE, Configuration.CONNECTION_TYPE_REMOTE);
    final EntityConnectionProvider connectionProvider = EntityConnectionProviders.connectionProvider(UNIT_TEST_USER, "test");
    assertEquals("RemoteEntityConnectionProvider", connectionProvider.getClass().getSimpleName());
    assertEquals(EntityConnection.Type.REMOTE, connectionProvider.getConnectionType());
    Configuration.setValue(Configuration.CLIENT_CONNECTION_TYPE, previousValue);
  }

  private static void configure() {
    Configuration.setValue(Configuration.REGISTRY_PORT, 2221);
    Configuration.setValue(Configuration.SERVER_PORT, 2223);
    Configuration.setValue(Configuration.SERVER_ADMIN_PORT, 2223);
    Configuration.setValue(Configuration.SERVER_ADMIN_USER, "scott:tiger");
    Configuration.setValue(Configuration.SERVER_HOST_NAME, "localhost");
    Configuration.setValue(Configuration.SERVER_CONNECTION_POOLING_INITIAL, UNIT_TEST_USER.getUsername() + ":" + UNIT_TEST_USER.getPassword());
    Configuration.setValue(Configuration.SERVER_DOMAIN_MODEL_CLASSES, "org.jminor.framework.db.remote.TestDomain");
    Configuration.setValue(Configuration.SERVER_CONNECTION_SSL_ENABLED, false);
    Configuration.setValue("java.rmi.server.hostname", "localhost");
  }
}
