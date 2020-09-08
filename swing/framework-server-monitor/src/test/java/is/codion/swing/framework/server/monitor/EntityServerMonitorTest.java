/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor;

import is.codion.common.db.database.Databases;
import is.codion.common.rmi.client.Clients;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.common.rmi.server.Server;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.user.User;
import is.codion.common.user.Users;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.rmi.RemoteEntityConnectionProvider;
import is.codion.framework.server.EntityServer;
import is.codion.framework.server.EntityServerAdmin;
import is.codion.framework.server.EntityServerConfiguration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.rmi.registry.LocateRegistry;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class EntityServerMonitorTest {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  private static final User ADMIN_USER = Users.parseUser("scott:tiger");
  private static Server<?, EntityServerAdmin> server;
  private static EntityServerAdmin admin;

  public static final EntityServerConfiguration CONFIGURATION = configure();

  @BeforeAll
  public static synchronized void setUp() throws Exception {
    EntityServer.startServer(CONFIGURATION);
    server = (Server) LocateRegistry.getRegistry(Clients.SERVER_HOST_NAME.get(),
            CONFIGURATION.getRegistryPort()).lookup(CONFIGURATION.getServerName());
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
            new RemoteEntityConnectionProvider("localhost", CONFIGURATION.getServerPort(), CONFIGURATION.getRegistryPort())
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

  private static EntityServerConfiguration configure() {
    Clients.SERVER_HOST_NAME.set("localhost");
    ServerConfiguration.RMI_SERVER_HOSTNAME.set("localhost");
    final EntityServerConfiguration configuration = EntityServerConfiguration.configuration(2223, 2221);
    configuration.setAdminPort(2223);
    configuration.setAdminUser(Users.parseUser("scott:tiger"));
    configuration.setStartupPoolUsers(Collections.singletonList(UNIT_TEST_USER));
    configuration.setDomainModelClassNames(Collections.singletonList(TestDomain.class.getName()));
    configuration.setDatabase(Databases.getInstance());
    configuration.setSslEnabled(false);

    return configuration;
  }
}
