/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.server.monitor;

import org.jminor.common.db.database.Databases;
import org.jminor.common.remote.server.RemoteClient;
import org.jminor.common.remote.server.Server;
import org.jminor.common.remote.server.ServerConfiguration;
import org.jminor.common.user.User;
import org.jminor.common.user.Users;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.remote.RemoteEntityConnectionProvider;
import org.jminor.framework.server.EntityConnectionServer;
import org.jminor.framework.server.EntityConnectionServerAdmin;
import org.jminor.framework.server.EntityConnectionServerConfiguration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.rmi.registry.LocateRegistry;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class EntityServerMonitorTest {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("jminor.test.user", "scott:tiger"));

  private static final User ADMIN_USER = Users.parseUser("scott:tiger");
  private static Server<?, EntityConnectionServerAdmin> server;
  private static EntityConnectionServerAdmin admin;

  public static final EntityConnectionServerConfiguration CONFIGURATION = configure();

  @BeforeAll
  public static synchronized void setUp() throws Exception {
    EntityConnectionServer.startServer(CONFIGURATION);
    server = (Server) LocateRegistry.getRegistry(Server.SERVER_HOST_NAME.get(),
            CONFIGURATION.getRegistryPort()).lookup(CONFIGURATION.getServerConfiguration().getServerName());
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
    final EntityConnectionProvider connectionProvider =
            new RemoteEntityConnectionProvider("localhost", CONFIGURATION.getServerConfiguration().getServerPort(), CONFIGURATION.getRegistryPort())
            .setDomainClassName("TestDomain").setClientTypeId(clientTypeId).setUser(UNIT_TEST_USER);
    connectionProvider.getConnection();
    final EntityServerMonitor model = new EntityServerMonitor("localhost", CONFIGURATION.getRegistryPort(), CONFIGURATION.getAdminUser());
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

  private static EntityConnectionServerConfiguration configure() {
    Server.SERVER_HOST_NAME.set("localhost");
    Server.RMI_SERVER_HOSTNAME.set("localhost");
    final ServerConfiguration serverConfiguration = ServerConfiguration.configuration(2223);
    final EntityConnectionServerConfiguration configuration = EntityConnectionServerConfiguration.configuration(serverConfiguration, 2221);
    configuration.setAdminPort(2223);
    configuration.setAdminUser(Users.parseUser("scott:tiger"));
    configuration.setStartupPoolUsers(Collections.singletonList(UNIT_TEST_USER));
    configuration.setDomainModelClassNames(Collections.singletonList(TestDomain.class.getName()));
    configuration.setSslEnabled(false);
    configuration.setDatabase(Databases.getInstance());

    return configuration;
  }
}
