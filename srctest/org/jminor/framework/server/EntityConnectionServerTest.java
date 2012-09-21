/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.Database;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.model.tools.MethodLogger;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.LoginProxy;
import org.jminor.common.server.ServerException;
import org.jminor.common.server.ServerLog;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.server.provider.RemoteEntityConnectionProvider;

import ch.qos.logback.classic.Level;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.UUID;

import static org.junit.Assert.*;

public class EntityConnectionServerTest {

  private static EntityConnectionServer server;
  private static EntityConnectionServerAdminImpl admin;

  public static EntityConnectionServerAdmin getServerAdmin() {
    return admin;
  }

  @BeforeClass
  public static synchronized void setUp() throws Exception {
    configure();
    EntityConnectionServerAdminImpl.startServer();
    EntityConnectionServerTest.admin = EntityConnectionServerAdminImpl.getInstance();
    EntityConnectionServerTest.server = admin.getServer();
  }

  @AfterClass
  public static synchronized void tearDown() throws Exception {
    EntityConnectionServerAdminImpl.shutdownServer();
    deconfigure();
    admin = null;
    server = null;
  }

  @Test(expected = RuntimeException.class)
  public void testWrongPassword() throws Exception {
    new RemoteEntityConnectionProvider(new User(User.UNIT_TEST_USER.getUsername(), "foobar"), UUID.randomUUID(), getClass().getSimpleName()).getConnection();
  }

  @Test
  public void test() throws Exception {
    final RemoteEntityConnectionProvider providerOne = new RemoteEntityConnectionProvider(User.UNIT_TEST_USER,
            UUID.randomUUID(), getClass().getSimpleName());
    final EntityConnection remoteConnectionOne = providerOne.getConnection();
    assertTrue(remoteConnectionOne.isValid());
    assertEquals(1, admin.getConnectionCount());
    admin.setPoolConnectionThreshold(User.UNIT_TEST_USER, 505);
    assertEquals(505, admin.getPoolConnectionThreshold(User.UNIT_TEST_USER));
    assertTrue(admin.isConnectionPoolEnabled(User.UNIT_TEST_USER));
    admin.setConnectionPoolEnabled(User.UNIT_TEST_USER, false);
    assertFalse(admin.isConnectionPoolEnabled(User.UNIT_TEST_USER));
    admin.setConnectionPoolEnabled(User.UNIT_TEST_USER, true);
    admin.setPooledConnectionTimeout(User.UNIT_TEST_USER, 60005);
    assertEquals(60005, admin.getPooledConnectionTimeout(User.UNIT_TEST_USER));
    admin.setMaximumPoolCheckOutTime(User.UNIT_TEST_USER, 2005);
    assertEquals(2005, admin.getMaximumPoolCheckOutTime(User.UNIT_TEST_USER));

    final RemoteEntityConnectionProvider providerTwo = new RemoteEntityConnectionProvider(User.UNIT_TEST_USER,
            UUID.randomUUID(), getClass().getSimpleName());
    final EntityConnection remoteConnectionTwo = providerTwo.getConnection();
    admin.setLoggingEnabled(providerOne.getClientID(), true);
    assertTrue(admin.isLoggingEnabled(providerOne.getClientID()));
    assertTrue(remoteConnectionTwo.isValid());
    assertEquals(2, admin.getConnectionCount());

    Collection<ClientInfo> clients = admin.getClients(new User(User.UNIT_TEST_USER.getUsername(), null));
    assertEquals(2, clients.size());
    clients = admin.getClients(getClass().getSimpleName());
    assertEquals(2, clients.size());
    final Collection<String> clientTypes = admin.getClientTypes();
    assertEquals(1, clientTypes.size());
    assertTrue(clientTypes.contains(getClass().getSimpleName()));

    final Collection<User> users = admin.getUsers();
    assertEquals(1, users.size());
    assertEquals(User.UNIT_TEST_USER, users.iterator().next());

    providerTwo.getConnection().selectAll(EmpDept.T_EMPLOYEE);

    final Database.Statistics stats = admin.getDatabaseStatistics();
    assertNotNull(stats.getTimestamp());
    assertNotNull(stats.getQueriesPerSecond());

    final ServerLog log = admin.getServerLog(providerTwo.getClientID());
    assertEquals("returnConnection", log.getLogger().getLastAccessedMethod());
    assertEquals("selectAll", log.getLogger().getLastExitedMethod());
    assertTrue(log.getLastDelta() >= 0);
    assertNotNull(log.getConnectionCreationDate());
    assertTrue(log.getLogger().getLastAccessTime() > 0);
    assertNotNull(log.getLogger().getLastAccessMessage());
    assertTrue(log.getLogger().getLastExitTime() > 0);

    final MethodLogger.Entry entry = log.getLogger().getFirstEntry();
    assertEquals("getConnection", entry.getMethod());
    assertTrue(entry.getDelta() >= 0);

    providerOne.disconnect();
    assertEquals(1, admin.getConnectionCount());

    providerTwo.disconnect();
    assertEquals(0, admin.getConnectionCount());

    server.setConnectionLimit(1);
    providerOne.getConnection();
    try {
      providerTwo.getConnection();
      fail("Server should be full");
    }
    catch (RuntimeException e) {}

    assertEquals(1, admin.getConnectionCount());
    admin.setConnectionLimit(2);
    providerTwo.getConnection();
    assertEquals(2, admin.getConnectionCount());

    providerOne.disconnect();
    assertEquals(1, admin.getConnectionCount());
    providerTwo.disconnect();
    assertEquals(0, admin.getConnectionCount());

    //testing with the EmpDeptLoginProxy
    admin.setConnectionLimit(3);
    assertEquals(3, admin.getConnectionLimit());
    final String empDeptClientTypeID = "org.jminor.framework.demos.empdept.client.ui.EmpDeptAppPanel";
    final RemoteEntityConnectionProvider empDeptProviderJohn = new RemoteEntityConnectionProvider(new User("john", "hello"),
            UUID.randomUUID(), empDeptClientTypeID);
    final RemoteEntityConnectionProvider empDeptProviderHelen = new RemoteEntityConnectionProvider(new User("helen", "juno"),
            UUID.randomUUID(), empDeptClientTypeID);
    final RemoteEntityConnectionProvider empDeptProviderInvalid = new RemoteEntityConnectionProvider(new User("foo", "bar"),
            UUID.randomUUID(), empDeptClientTypeID);
    empDeptProviderJohn.getConnection();
    empDeptProviderHelen.getConnection();
    try {
      empDeptProviderInvalid.getConnection();
      fail("Should not be able to connect with an invalid user");
    }
    catch (Exception e) {}
    final Collection<ClientInfo> empDeptClients = admin.getClients(empDeptClientTypeID);
    assertEquals(2, empDeptClients.size());
    for (final ClientInfo empDeptClient : empDeptClients) {
      assertEquals(User.UNIT_TEST_USER, empDeptClient.getDatabaseUser());
    }
    empDeptProviderJohn.disconnect();
    assertEquals(1, admin.getConnectionCount());
    empDeptProviderHelen.disconnect();
    assertEquals(0, admin.getConnectionCount());

    try {
      admin.setConnectionTimeout(-1);
      fail();
    }
    catch (IllegalArgumentException ignored) {}
  }

  @Test
  public void testWebServer() throws URISyntaxException, IOException {
    InputStream input = null;
    try {
      input = new URL("http://localhost:12345/file_templates/EntityEditPanel.template").openStream();
      assertTrue(input.read() > 0);
    }
    finally {
      Util.closeSilently(input);
    }
  }

  @Test
  public void testLoginProxy() throws ServerException.ServerFullException, ServerException.LoginException, RemoteException {
    final String clientTypeID = "loginProxyTestClient";
    //create login proxy which returns clientinfo with databaseUser scott:tiger for authenticated users
    final LoginProxy proxy = new LoginProxy() {
      @Override
      public String getClientTypeID() {
        return clientTypeID;
      }
      @Override
      public ClientInfo doLogin(final ClientInfo clientInfo) throws ServerException.LoginException {
        return new ClientInfo(clientInfo.getClientID(), clientInfo.getClientTypeID(), clientInfo.getUser(), User.UNIT_TEST_USER);
      }
      @Override
      public void close() {}
    };

    server.setLoginProxy(clientTypeID, proxy);

    final User userOne = new User("foo", "bar");
    final ClientInfo clientOne = new ClientInfo(UUID.randomUUID(), clientTypeID, userOne);

    final User userTwo = new User("bar", "foo");
    final ClientInfo clientTwo = new ClientInfo(UUID.randomUUID(), clientTypeID, userTwo);

    final RemoteEntityConnection connectionOne = server.connect(clientOne);
    assertEquals(userOne, connectionOne.getUser());

    Collection<ClientInfo> clients = server.getClients(clientTypeID);
    assertEquals(1, clients.size());
    final ClientInfo clientOneFromServer = clients.iterator().next();
    assertEquals(userOne, clientOneFromServer.getUser());
    assertEquals(User.UNIT_TEST_USER, clientOneFromServer.getDatabaseUser());

    final RemoteEntityConnection connectionTwo = server.connect(clientTwo);
    assertEquals(userTwo, connectionTwo.getUser());

    clients = server.getClients(clientTypeID);
    assertEquals(2, clients.size());

    boolean found = false;
    for (final ClientInfo clientInfo : server.getClients(clientTypeID)) {
      if (clientInfo.equals(clientTwo)) {
        found = true;
        assertEquals(User.UNIT_TEST_USER, clientInfo.getDatabaseUser());
      }
    }
    assertTrue("Client two should have been returned from server", found);

    server.disconnect(clientOne.getClientID());
    server.disconnect(clientTwo.getClientID());
  }

  @Test
  public void coverAdmin() throws RemoteException {
    final EntityConnectionServerAdmin admin = getServerAdmin();
    admin.setWarningTimeThreshold(300);
    assertEquals(300, admin.getWarningTimeThreshold());
    admin.getActiveConnectionCount();
    admin.getAllocatedMemory();
    admin.setConnectionTimeout(30);
    assertEquals(30, admin.getConnectionTimeout());
    admin.getDatabaseStatistics();
    admin.getDatabaseURL();
    admin.getEnabledConnectionPools();
    admin.getEntityDefinitions();
    admin.setLoggingLevel(Level.INFO);
    assertEquals(Level.INFO, admin.getLoggingLevel());
    admin.setMaintenanceInterval(500);
    assertEquals(500, admin.getMaintenanceInterval());
    admin.getMaxMemory();
    admin.getMemoryUsage();
    admin.getRequestsPerSecond();
    admin.getServerName();
    admin.getServerPort();
    admin.getServerVersion();
    admin.getStartDate();
    admin.getSystemProperties();
    admin.getUsedMemory();
    admin.getUsers();
    admin.getWarningTimeExceededPerSecond();
    admin.getWarningTimeThreshold();
  }

  private static void configure() {
    Configuration.setValue(Configuration.SERVER_PORT, 2222);
    Configuration.setValue(Configuration.SERVER_HOST_NAME, "localhost");
    Configuration.setValue(Configuration.SERVER_CONNECTION_POOLING_INITIAL, User.UNIT_TEST_USER.getUsername() + ":" + User.UNIT_TEST_USER.getPassword());
    Configuration.setValue(Configuration.SERVER_DOMAIN_MODEL_CLASSES, "org.jminor.framework.demos.empdept.domain.EmpDept");
    Configuration.setValue(Configuration.SERVER_LOGIN_PROXY_CLASSES, "org.jminor.framework.demos.empdept.server.EmpDeptLoginProxy");
    Configuration.setValue(Configuration.WEB_SERVER_DOCUMENT_ROOT, System.getProperty("user.dir") + System.getProperty("file.separator") + "resources");
    Configuration.setValue(Configuration.WEB_SERVER_PORT, 12345);
    Configuration.setValue("java.rmi.server.hostname", "localhost");
    Configuration.setValue("java.security.policy", "resources/security/all_permissions.policy");
    Configuration.setValue("javax.net.ssl.trustStore", "resources/security/JMinorClientTruststore");
    Configuration.setValue("javax.net.ssl.keyStore", "resources/security/JMinorServerKeystore");
    Configuration.setValue("javax.net.ssl.keyStorePassword", "jminor");
  }

  private static void deconfigure() {
    Configuration.clearValue(Configuration.SERVER_PORT);
    Configuration.clearValue(Configuration.SERVER_HOST_NAME);
    Configuration.clearValue(Configuration.SERVER_CONNECTION_POOLING_INITIAL);
    Configuration.clearValue(Configuration.SERVER_DOMAIN_MODEL_CLASSES);
    Configuration.clearValue(Configuration.SERVER_LOGIN_PROXY_CLASSES);
    Configuration.clearValue(Configuration.WEB_SERVER_DOCUMENT_ROOT);
    Configuration.clearValue(Configuration.WEB_SERVER_PORT);
    Configuration.clearValue("java.rmi.server.hostname");
    Configuration.clearValue("java.security.policy");
    Configuration.clearValue("javax.net.ssl.trustStore");
    Configuration.clearValue("javax.net.ssl.keyStore");
    Configuration.clearValue("javax.net.ssl.keyStorePassword");
  }
}
