/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.MethodLogger;
import org.jminor.common.User;
import org.jminor.common.db.Database;
import org.jminor.common.db.Databases;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.ClientLog;
import org.jminor.common.server.Server;
import org.jminor.common.server.ServerException;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.db.remote.RemoteEntityConnectionProvider;

import ch.qos.logback.classic.Level;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Collection;
import java.util.UUID;

import static org.junit.Assert.*;

public class DefaultEntityConnectionServerTest {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger"));

  private static final int WEB_SERVER_PORT_NUMBER = 8089;

  private static final User ADMIN_USER = new User("scott", "tiger");
  private static Server server;
  private static EntityConnectionServerAdmin admin;

  public static EntityConnectionServerAdmin getServerAdmin() {
    return admin;
  }

  @BeforeClass
  public static synchronized void setUp() throws Exception {
    TestDomain.init();
    configure();
    final Database database = Databases.createInstance();
    final String serverName = DefaultEntityConnectionServer.initializeServerName(database.getHost(), database.getSid());
    DefaultEntityConnectionServer.startServer();
    server = (Server) LocateRegistry.getRegistry(Server.SERVER_HOST_NAME.get(), Server.REGISTRY_PORT.get()).lookup(serverName);
    admin = (EntityConnectionServerAdmin) server.getServerAdmin(ADMIN_USER);
  }

  @AfterClass
  public static synchronized void tearDown() throws Exception {
    admin.shutdown();
    server = null;
  }

  @Test(expected = RuntimeException.class)
  public void testWrongPassword() throws Exception {
    new RemoteEntityConnectionProvider("localhost", new User(UNIT_TEST_USER.getUsername(), "foobar"),
            UUID.randomUUID(), getClass().getSimpleName()).getConnection();
  }

  @Test(expected = ServerException.AuthenticationException.class)
  public void getServerAdminEmptyPassword() throws Exception {
    server.getServerAdmin(new User("test", ""));
  }

  @Test(expected = ServerException.AuthenticationException.class)
  public void getServerAdminNullPassword() throws Exception {
    server.getServerAdmin(new User("test", null));
  }

  @Test(expected = ServerException.AuthenticationException.class)
  public void getServerAdminWrongPassword() throws Exception {
    server.getServerAdmin(new User("test", "test"));
  }

  @Test(expected = ServerException.AuthenticationException.class)
  public void getServerAdminEmptyUsername() throws Exception {
    server.getServerAdmin(new User("", "test"));
  }

  @Test(expected = ServerException.AuthenticationException.class)
  public void getServerAdminWrongUsername() throws Exception {
    server.getServerAdmin(new User("test", "test"));
  }

  @Test
  public void test() throws Exception {
    final RemoteEntityConnectionProvider providerOne = new RemoteEntityConnectionProvider("localhost",
            UNIT_TEST_USER, UUID.randomUUID(), getClass().getSimpleName());
    final EntityConnection remoteConnectionOne = providerOne.getConnection();
    assertTrue(remoteConnectionOne.isConnected());
    assertEquals(1, admin.getConnectionCount());
    admin.setPoolConnectionThreshold(UNIT_TEST_USER, 505);
    assertEquals(505, admin.getPoolConnectionThreshold(UNIT_TEST_USER));
    admin.setPooledConnectionTimeout(UNIT_TEST_USER, 60005);
    assertEquals(60005, admin.getPooledConnectionTimeout(UNIT_TEST_USER));
    admin.setMaximumPoolCheckOutTime(UNIT_TEST_USER, 2005);
    assertEquals(2005, admin.getMaximumPoolCheckOutTime(UNIT_TEST_USER));

    final RemoteEntityConnectionProvider providerTwo = new RemoteEntityConnectionProvider("localhost",
            UNIT_TEST_USER, UUID.randomUUID(), getClass().getSimpleName());
    final EntityConnection remoteConnectionTwo = providerTwo.getConnection();
    admin.setLoggingEnabled(providerOne.getClientID(), true);
    assertTrue(admin.isLoggingEnabled(providerOne.getClientID()));
    assertFalse(admin.isLoggingEnabled(UUID.randomUUID()));
    admin.setLoggingEnabled(UUID.randomUUID(), true);
    assertTrue(remoteConnectionTwo.isConnected());
    assertEquals(2, admin.getConnectionCount());
    assertEquals(2, admin.getClients().size());

    Collection<ClientInfo> clients = admin.getClients(new User(UNIT_TEST_USER.getUsername(), null));
    assertEquals(2, clients.size());
    clients = admin.getClients(getClass().getSimpleName());
    assertEquals(2, clients.size());
    final Collection<String> clientTypes = admin.getClientTypes();
    assertEquals(1, clientTypes.size());
    assertTrue(clientTypes.contains(getClass().getSimpleName()));

    final Collection<User> users = admin.getUsers();
    assertEquals(1, users.size());
    assertEquals(UNIT_TEST_USER, users.iterator().next());

    providerTwo.getConnection().selectMany(EntityConditions.selectCondition(TestDomain.T_EMP)
            .orderByAscending(TestDomain.EMP_NAME));

    final Database.Statistics stats = admin.getDatabaseStatistics();
    assertNotNull(stats.getTimestamp());
    assertNotNull(stats.getQueriesPerSecond());

    final ClientLog log = admin.getClientLog(providerTwo.getClientID());
    assertNotNull(log.getConnectionCreationDate());
    assertNull(admin.getClientLog(UUID.randomUUID()));

    final MethodLogger.Entry entry = log.getEntries().get(0);
    assertEquals("selectMany", entry.getMethod());
    assertTrue(entry.getDelta() >= 0);

    admin.removeConnections(true);

    providerOne.disconnect();
    assertEquals(1, admin.getConnectionCount());

    providerTwo.disconnect();
    assertEquals(0, admin.getConnectionCount());

    admin.setConnectionLimit(1);
    providerOne.getConnection();
    try {
      providerTwo.getConnection();
      fail("Server should be full");
    }
    catch (final RuntimeException ignored) {/*ignored*/}

    assertEquals(1, admin.getConnectionCount());
    admin.setConnectionLimit(2);
    providerTwo.getConnection();
    assertEquals(2, admin.getConnectionCount());

    providerOne.disconnect();
    assertEquals(1, admin.getConnectionCount());
    providerTwo.disconnect();
    assertEquals(0, admin.getConnectionCount());

    //testing with the TestLoginProxy
    admin.setConnectionLimit(3);
    assertEquals(3, admin.getConnectionLimit());
    final String testClientTypeID = "TestLoginProxy";
    final RemoteEntityConnectionProvider testProviderJohn = new RemoteEntityConnectionProvider("localhost",
            new User("john", "hello"), UUID.randomUUID(), testClientTypeID);
    final RemoteEntityConnectionProvider testProviderHelen = new RemoteEntityConnectionProvider("localhost",
            new User("helen", "juno"), UUID.randomUUID(), testClientTypeID);
    final RemoteEntityConnectionProvider testProviderInvalid = new RemoteEntityConnectionProvider("localhost",
            new User("foo", "bar"), UUID.randomUUID(), testClientTypeID);
    testProviderJohn.getConnection();
    testProviderHelen.getConnection();
    try {
      testProviderInvalid.getConnection();
      fail("Should not be able to connect with an invalid user");
    }
    catch (final Exception ignored) {/*ignored*/}
    final Collection<ClientInfo> empDeptClients = admin.getClients(testClientTypeID);
    assertEquals(2, empDeptClients.size());
    for (final ClientInfo empDeptClient : empDeptClients) {
      assertEquals(UNIT_TEST_USER, empDeptClient.getDatabaseUser());
    }
    testProviderJohn.disconnect();
    assertEquals(1, admin.getConnectionCount());
    testProviderHelen.disconnect();
    assertEquals(0, admin.getConnectionCount());
  }

  @Test
  public void coverAdmin() throws RemoteException {
    admin.getActiveConnectionCount();
    admin.getAllocatedMemory();
    admin.setConnectionTimeout(30);
    try {
      admin.setConnectionTimeout(-1);
      fail();
    }
    catch (final IllegalArgumentException ignored) {/*ignored*/}
    assertEquals(30, admin.getConnectionTimeout());
    admin.getDatabaseStatistics();
    admin.getDatabaseURL();
    admin.getConnectionPools();
    admin.getEntityDefinitions();
    admin.setLoggingLevel(Level.INFO);
    assertEquals(Level.INFO, admin.getLoggingLevel());
    admin.setMaintenanceInterval(500);
    assertEquals(500, admin.getMaintenanceInterval());
    admin.getMaxMemory();
    admin.getRequestsPerSecond();
    admin.getThreadStatistics();
    admin.getGcEvents();
    admin.getServerInfo();
    admin.getSystemProperties();
    admin.getUsedMemory();
    admin.getUsers();
  }

  private static void configure() {
    Server.REGISTRY_PORT.set(2221);
    Server.SERVER_PORT.set(2223);
    Server.SERVER_HOST_NAME.set("localhost");
    Server.SERVER_ADMIN_PORT.set(2223);
    Server.SERVER_ADMIN_USER.set("scott:tiger");
    DefaultEntityConnectionServer.SERVER_CONNECTION_POOLING_INITIAL.set(UNIT_TEST_USER.getUsername() + ":" + UNIT_TEST_USER.getPassword());
    DefaultEntityConnectionServer.SERVER_CLIENT_CONNECTION_TIMEOUT.set("ClientTypeID:10000");
    DefaultEntityConnectionServer.SERVER_DOMAIN_MODEL_CLASSES.set("org.jminor.framework.server.TestDomain");
    DefaultEntityConnectionServer.SERVER_LOGIN_PROXY_CLASSES.set("org.jminor.framework.server.TestLoginProxy");
    DefaultEntityConnectionServer.SERVER_CONNECTION_VALIDATOR_CLASSES.set("org.jminor.framework.server.TestConnectionValidator");
    DefaultEntityConnectionServer.SERVER_CLIENT_LOGGING_ENABLED.set(true);
    DefaultEntityConnectionServer.WEB_SERVER_PORT.set(2224);
    DefaultEntityConnectionServer.WEB_SERVER_DOCUMENT_ROOT.set(System.getProperty("user.dir"));
    DefaultEntityConnectionServer.WEB_SERVER_IMPLEMENTATION_CLASS.set(TestWebServer.class.getName());
    Server.RMI_SERVER_HOSTNAME.set("localhost");
    System.setProperty("java.security.policy", "resources/security/all_permissions.policy");
    Server.TRUSTSTORE.set("resources/security/JMinorClientTruststore");
    System.setProperty("javax.net.ssl.keyStore", "resources/security/JMinorServerKeystore");
    System.setProperty("javax.net.ssl.keyStorePassword", "crappypass");
  }

  public static final class TestWebServer implements Server.AuxiliaryServer {

    public TestWebServer(final Server connectionServer, final String documentRoot, final Integer port) {}

    @Override
    public void startServer() throws Exception {}

    @Override
    public void stopServer() throws Exception {}
  }
}
