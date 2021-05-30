/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor;

import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.rmi.client.Clients;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.common.rmi.server.Server;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.user.User;
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
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  private static final User ADMIN_USER = User.parseUser("scott:tiger");
  private static Server<?, EntityServerAdmin> server;
  private static EntityServerAdmin admin;

  public static final EntityServerConfiguration CONFIGURATION = configure();

  @BeforeAll
  public static synchronized void setUp() throws Exception {
    EntityServer.startServer(CONFIGURATION);
    server = (Server<?, EntityServerAdmin>) LocateRegistry.getRegistry(Clients.SERVER_HOST_NAME.get(),
            CONFIGURATION.getRegistryPort()).lookup(CONFIGURATION.getServerName());
    admin = server.getServerAdmin(ADMIN_USER);
  }

  @AfterAll
  public static synchronized void tearDown() throws Exception {
    admin.shutdown();
    server = null;
  }

  @Test
  void test() throws Exception {
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
    assertEquals(1, clientMonitor.getRemoteClientListModel().size());
    final RemoteClient remoteClient = clientMonitor.getRemoteClientListModel().firstElement();
    assertEquals(connectionProvider.getClientId(), remoteClient.getClientId());
    assertEquals(UNIT_TEST_USER, remoteClient.getUser());

    clientMonitor.getServer().disconnect(remoteClient.getClientId());//disconnects the client

    clientMonitor.refresh();
    assertTrue(clientMonitor.getRemoteClientListModel().isEmpty());
    clientUserMonitor.refresh();
    assertTrue(clientUserMonitor.getUserListModel().isEmpty());
    assertTrue(clientUserMonitor.getClientTypeListModel().isEmpty());

    serverMonitor.shutdown();
  }

  private static EntityServerConfiguration configure() {
    Clients.SERVER_HOST_NAME.set("localhost");
    ServerConfiguration.RMI_SERVER_HOSTNAME.set("localhost");
    final EntityServerConfiguration configuration = EntityServerConfiguration.configuration(3223, 3221);
    configuration.setServerAdminPort(3223);
    configuration.setAdminUser(User.parseUser("scott:tiger"));
    configuration.setStartupPoolUsers(Collections.singletonList(UNIT_TEST_USER));
    configuration.setDomainModelClassNames(Collections.singletonList(TestDomain.class.getName()));
    configuration.setDatabase(DatabaseFactory.getDatabase());
    configuration.setSslEnabled(false);

    return configuration;
  }
}
