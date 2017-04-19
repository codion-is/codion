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
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.db.remote.RemoteEntityConnectionProvider;
import org.jminor.framework.domain.TestDomain;

import ch.qos.logback.classic.Level;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
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
    configure();
    final Database database = Databases.createInstance();
    final String serverName = DefaultEntityConnectionServer.initializeServerName(database.getHost(), database.getSid());
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
    Configuration.setValue(Configuration.REGISTRY_PORT, 2221);
    Configuration.setValue(Configuration.SERVER_PORT, 2223);
    Configuration.setValue(Configuration.SERVER_ADMIN_PORT, 2223);
    Configuration.setValue(Configuration.SERVER_ADMIN_USER, "scott:tiger");
    Configuration.setValue(Configuration.SERVER_HOST_NAME, "localhost");
    Configuration.setValue(Configuration.SERVER_CONNECTION_POOLING_INITIAL, UNIT_TEST_USER.getUsername() + ":" + UNIT_TEST_USER.getPassword());
    Configuration.setValue(Configuration.SERVER_CLIENT_CONNECTION_TIMEOUT, "ClientTypeID:10000");
    Configuration.setValue(Configuration.SERVER_DOMAIN_MODEL_CLASSES, "org.jminor.framework.domain.TestDomain");
    Configuration.setValue(Configuration.SERVER_LOGIN_PROXY_CLASSES, "org.jminor.framework.server.TestLoginProxy");
    Configuration.setValue(Configuration.SERVER_CONNECTION_VALIDATOR_CLASSES, "org.jminor.framework.server.TestConnectionValidator");
    Configuration.setValue(Configuration.SERVER_CLIENT_LOGGING_ENABLED, true);
    Configuration.setValue("java.rmi.server.hostname", "localhost");
    Configuration.setValue("java.security.policy", "resources/security/all_permissions.policy");
    Configuration.setValue("javax.net.ssl.trustStore", "resources/security/JMinorClientTruststore");
    Configuration.setValue("javax.net.ssl.keyStore", "resources/security/JMinorServerKeystore");
    Configuration.setValue("javax.net.ssl.keyStorePassword", "crappypass");
  }

  private static void deconfigure() {
    Configuration.setValue(Configuration.REGISTRY_PORT, Registry.REGISTRY_PORT);
    Configuration.clearValue(Configuration.SERVER_PORT);
    Configuration.clearValue(Configuration.SERVER_ADMIN_PORT);
    Configuration.clearValue(Configuration.SERVER_ADMIN_USER);
    Configuration.clearValue(Configuration.SERVER_HOST_NAME);
    Configuration.clearValue(Configuration.SERVER_CONNECTION_POOLING_INITIAL);
    Configuration.clearValue(Configuration.SERVER_CLIENT_CONNECTION_TIMEOUT);
    Configuration.clearValue(Configuration.SERVER_DOMAIN_MODEL_CLASSES);
    Configuration.clearValue(Configuration.SERVER_LOGIN_PROXY_CLASSES);
    Configuration.clearValue(Configuration.SERVER_CONNECTION_VALIDATOR_CLASSES);
    Configuration.setValue(Configuration.SERVER_CLIENT_LOGGING_ENABLED, false);
    Configuration.clearValue("java.rmi.server.hostname");
    Configuration.clearValue("java.security.policy");
    Configuration.clearValue("javax.net.ssl.trustStore");
    Configuration.clearValue("javax.net.ssl.keyStore");
    Configuration.clearValue("javax.net.ssl.keyStorePassword");
  }
}
