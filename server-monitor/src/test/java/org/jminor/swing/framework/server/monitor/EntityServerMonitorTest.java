/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.server.monitor;

import org.jminor.common.User;
import org.jminor.common.Version;
import org.jminor.common.db.Database;
import org.jminor.common.db.Databases;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.Server;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.remote.RemoteEntityConnectionProvider;
import org.jminor.framework.server.DefaultEntityConnectionServer;
import org.jminor.framework.server.EntityConnectionServerAdmin;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.rmi.registry.LocateRegistry;
import java.util.UUID;

import static org.junit.Assert.*;

public class EntityServerMonitorTest {

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
    final UUID clientId = UUID.randomUUID();
    final String clientTypeId = EntityServerMonitorTest.class.getName();
    final RemoteEntityConnectionProvider connectionProvider = new RemoteEntityConnectionProvider("localhost",
            UNIT_TEST_USER, clientId, clientTypeId);
    connectionProvider.getConnection();
    final EntityServerMonitor model = new EntityServerMonitor("localhost", Configuration.getIntValue(Configuration.REGISTRY_PORT));
    model.refresh();
    final HostMonitor hostMonitor = model.getHostMonitors().iterator().next();
    assertEquals("localhost", hostMonitor.getHostName());
    hostMonitor.refresh();
    final ServerMonitor serverMonitor = hostMonitor.getServerMonitors().iterator().next();
    assertNotNull(serverMonitor);
    final ClientUserMonitor clientUserMonitor = serverMonitor.getClientMonitor();
    clientUserMonitor.refresh();
    assertEquals(1, clientUserMonitor.getUserListModel().size());
    assertEquals(1, clientUserMonitor.getClientTypeListModel().size());
    final ClientMonitor clientMonitor = clientUserMonitor.getClientTypeListModel().firstElement();
    assertEquals(clientTypeId, clientMonitor.getClientTypeID());
    clientMonitor.refresh();
    assertEquals(1, clientMonitor.getClientInstanceListModel().size());
    final ClientInstanceMonitor clientInstanceMonitor = clientMonitor.getClientInstanceListModel().firstElement();
    final ClientInfo clientInfo = clientInstanceMonitor.getClientInfo();
    assertEquals(clientId, clientInfo.getClientID());
    assertEquals(UNIT_TEST_USER, clientInfo.getUser());

    clientInstanceMonitor.disconnect();//disconnects the client

    clientMonitor.refresh();
    assertTrue(clientMonitor.getClientInstanceListModel().isEmpty());
    clientUserMonitor.refresh();
    assertTrue(clientUserMonitor.getUserListModel().isEmpty());
    assertTrue(clientUserMonitor.getClientTypeListModel().isEmpty());

    serverMonitor.shutdown();
  }

  private static void configure() {
    Configuration.setValue(Configuration.REGISTRY_PORT, 2221);
    Configuration.setValue(Configuration.SERVER_PORT, 2223);
    Configuration.setValue(Configuration.SERVER_ADMIN_PORT, 2223);
    Configuration.setValue(Configuration.SERVER_ADMIN_USER, "scott:tiger");
    Configuration.setValue(Configuration.SERVER_HOST_NAME, "localhost");
    Configuration.setValue(Configuration.SERVER_CONNECTION_POOLING_INITIAL, UNIT_TEST_USER.getUsername() + ":" + UNIT_TEST_USER.getPassword());
    Configuration.setValue(Configuration.SERVER_DOMAIN_MODEL_CLASSES, "org.jminor.swing.framework.server.monitor.TestDomain");
    Configuration.setValue(Configuration.SERVER_CONNECTION_SSL_ENABLED, false);
    Configuration.setValue("java.rmi.server.hostname", "localhost");
  }
}
