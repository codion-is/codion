/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
    String clientTypeId = EntityServerMonitorTest.class.getName();
    EntityConnectionProvider connectionProvider =
            new RemoteEntityConnectionProvider("localhost", CONFIGURATION.getServerPort(), CONFIGURATION.getRegistryPort())
            .setDomainClassName("TestDomain").setClientTypeId(clientTypeId).setUser(UNIT_TEST_USER);
    connectionProvider.getConnection();
    EntityServerMonitor model = new EntityServerMonitor("localhost", CONFIGURATION.getRegistryPort(), CONFIGURATION.getAdminUser());
    model.refresh();
    HostMonitor hostMonitor = model.getHostMonitors().iterator().next();
    assertEquals("localhost", hostMonitor.getHostName());
    hostMonitor.refresh();
    ServerMonitor serverMonitor = hostMonitor.getServerMonitors().iterator().next();
    assertNotNull(serverMonitor);
    ClientUserMonitor clientUserMonitor = serverMonitor.getClientMonitor();
    clientUserMonitor.refresh();
    assertEquals(1, clientUserMonitor.getUserListModel().size());
    assertEquals(1, clientUserMonitor.getClientTypeListModel().size());
    ClientMonitor clientMonitor = clientUserMonitor.getClientTypeListModel().firstElement();
    assertEquals(clientTypeId, clientMonitor.getClientTypeId());
    clientMonitor.refresh();
    assertEquals(1, clientMonitor.getRemoteClientListModel().size());
    RemoteClient remoteClient = clientMonitor.getRemoteClientListModel().firstElement();
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

    return EntityServerConfiguration.builder(3223, 3221)
            .adminPort(3223)
            .adminUser(User.parseUser("scott:tiger"))
            .startupPoolUsers(Collections.singletonList(UNIT_TEST_USER))
            .domainModelClassNames(Collections.singletonList(TestDomain.class.getName()))
            .database(DatabaseFactory.getDatabase())
            .sslEnabled(false)
            .build();
  }
}
