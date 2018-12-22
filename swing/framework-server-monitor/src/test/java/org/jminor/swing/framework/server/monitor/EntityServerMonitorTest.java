/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.server.monitor;

import org.jminor.common.User;
import org.jminor.common.Version;
import org.jminor.common.db.Database;
import org.jminor.common.db.Databases;
import org.jminor.common.server.RemoteClient;
import org.jminor.common.server.Server;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.remote.RemoteEntityConnectionProvider;
import org.jminor.framework.server.DefaultEntityConnectionServer;
import org.jminor.framework.server.EntityConnectionServerAdmin;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.rmi.registry.LocateRegistry;

import static org.junit.jupiter.api.Assertions.*;

public class EntityServerMonitorTest {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  private static final User ADMIN_USER = new User("scott", "tiger".toCharArray());
  private static Server<?, EntityConnectionServerAdmin> server;
  private static EntityConnectionServerAdmin admin;

  @BeforeAll
  public static synchronized void setUp() throws Exception {
    configure();
    final Database database = Databases.getInstance();
    final String serverName = Server.SERVER_NAME_PREFIX.get() + " " + Version.getVersionString()
            + "@" + (database.getSid() != null ? database.getSid().toUpperCase() : database.getHost().toUpperCase());
    DefaultEntityConnectionServer.startServer();
    server = (Server) LocateRegistry.getRegistry(Server.SERVER_HOST_NAME.get(),
            Server.REGISTRY_PORT.get()).lookup(serverName);
    admin = server.getServerAdmin(ADMIN_USER);
  }

  @AfterAll
  public static synchronized void tearDown() throws Exception {
    admin.shutdown();
    server = null;
  }

  @Test
  public void test() throws Exception {
    final String clientTypeId = EntityServerMonitorTest.class.getName();
    final EntityConnectionProvider connectionProvider = new RemoteEntityConnectionProvider()
            .setDomainClassName("TestDomain").setClientTypeId(clientTypeId).setUser(UNIT_TEST_USER);
    connectionProvider.getConnection();
    final EntityServerMonitor model = new EntityServerMonitor("localhost", Server.REGISTRY_PORT.get());
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
    assertEquals(clientTypeId, clientMonitor.getClientTypeId());
    clientMonitor.refresh();
    assertEquals(1, clientMonitor.getClientInstanceListModel().size());
    final ClientInstanceMonitor clientInstanceMonitor = clientMonitor.getClientInstanceListModel().firstElement();
    final RemoteClient remoteClient = clientInstanceMonitor.getRemoteClient();
    assertEquals(connectionProvider.getClientId(), remoteClient.getClientId());
    assertEquals(UNIT_TEST_USER, remoteClient.getUser());

    clientInstanceMonitor.disconnect();//disconnects the client

    clientMonitor.refresh();
    assertTrue(clientMonitor.getClientInstanceListModel().isEmpty());
    clientUserMonitor.refresh();
    assertTrue(clientUserMonitor.getUserListModel().isEmpty());
    assertTrue(clientUserMonitor.getClientTypeListModel().isEmpty());

    serverMonitor.shutdown();
  }

  private static void configure() {
    Server.REGISTRY_PORT.set(2221);
    Server.SERVER_PORT.set(2223);
    Server.SERVER_ADMIN_PORT.set(2223);
    Server.SERVER_ADMIN_USER.set("scott:tiger");
    Server.SERVER_HOST_NAME.set("localhost");
    DefaultEntityConnectionServer.SERVER_CONNECTION_POOLING_STARTUP_POOL_USERS.set(UNIT_TEST_USER.getUsername()
            + ":" + String.valueOf(UNIT_TEST_USER.getPassword()));
    DefaultEntityConnectionServer.SERVER_DOMAIN_MODEL_CLASSES.set(TestDomain.class.getName());
    Server.SERVER_CONNECTION_SSL_ENABLED.set(false);
    Server.RMI_SERVER_HOSTNAME.set("localhost");
  }
}
