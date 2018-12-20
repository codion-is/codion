/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.MethodLogger;
import org.jminor.common.User;
import org.jminor.common.db.Database;
import org.jminor.common.db.Databases;
import org.jminor.common.i18n.Messages;
import org.jminor.common.server.ClientLog;
import org.jminor.common.server.Clients;
import org.jminor.common.server.ConnectionRequest;
import org.jminor.common.server.RemoteClient;
import org.jminor.common.server.Server;
import org.jminor.common.server.ServerException;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.db.condition.EntitySelectCondition;
import org.jminor.framework.db.remote.RemoteEntityConnection;
import org.jminor.framework.db.remote.RemoteEntityConnectionProvider;
import org.jminor.framework.domain.Entities;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultEntityConnectionServerTest {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  private static final User ADMIN_USER = new User("scott", "tiger".toCharArray());
  private static final Map<String, Object> CONNECTION_PARAMS =
          Collections.singletonMap(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_ID, "TestDomain");
  private static Server<RemoteEntityConnection, EntityConnectionServerAdmin> server;
  private static EntityConnectionServerAdmin admin;

  @BeforeAll
  public static synchronized void setUp() throws Exception {
    configure();
    final Database database = Databases.getInstance();
    final String serverName = DefaultEntityConnectionServer.initializeServerName(database.getHost(), database.getSid());
    DefaultEntityConnectionServer.startServer();
    server = (Server) LocateRegistry.getRegistry(Server.SERVER_HOST_NAME.get(), Server.REGISTRY_PORT.get()).lookup(serverName);
    admin = server.getServerAdmin(ADMIN_USER);
  }

  @AfterAll
  public static synchronized void tearDown() throws Exception {
    admin.shutdown();
    server = null;
  }

  @Test
  public void testWrongPassword() throws Exception {
    assertThrows(ServerException.AuthenticationException.class, () -> server.connect(Clients.connectionRequest(new User(UNIT_TEST_USER.getUsername(), "foobar".toCharArray()),
            UUID.randomUUID(), getClass().getSimpleName(), CONNECTION_PARAMS)));
  }

  @Test
  public void getServerAdminEmptyPassword() throws Exception {
    assertThrows(ServerException.AuthenticationException.class, () -> server.getServerAdmin(new User("test", "".toCharArray())));
  }

  @Test
  public void getServerAdminNullPassword() throws Exception {
    assertThrows(ServerException.AuthenticationException.class, () -> server.getServerAdmin(new User("test", null)));
  }

  @Test
  public void getServerAdminWrongPassword() throws Exception {
    assertThrows(ServerException.AuthenticationException.class, () -> server.getServerAdmin(new User("test", "test".toCharArray())));
  }

  @Test
  public void getServerAdminEmptyUsername() throws Exception {
    assertThrows(ServerException.AuthenticationException.class, () -> server.getServerAdmin(new User("", "test".toCharArray())));
  }

  @Test
  public void getServerAdminWrongUsername() throws Exception {
    assertThrows(ServerException.AuthenticationException.class, () -> server.getServerAdmin(new User("test", "test".toCharArray())));
  }

  @Test
  public void test() throws Exception {
    final ConnectionRequest connectionRequestOne = Clients.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(),
            "ClientTypeID", CONNECTION_PARAMS);

    final RemoteEntityConnection remoteConnectionOne = server.connect(connectionRequestOne);
    assertTrue(remoteConnectionOne.isConnected());
    assertEquals(1, admin.getConnectionCount());
    admin.setPoolConnectionThreshold(UNIT_TEST_USER, 505);
    assertEquals(505, admin.getPoolConnectionThreshold(UNIT_TEST_USER));
    admin.setPooledConnectionTimeout(UNIT_TEST_USER, 60005);
    assertEquals(60005, admin.getPooledConnectionTimeout(UNIT_TEST_USER));
    admin.setMaximumPoolCheckOutTime(UNIT_TEST_USER, 2005);
    assertEquals(2005, admin.getMaximumPoolCheckOutTime(UNIT_TEST_USER));

    try {
      server.connect(Clients.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(), "ClientTypeID"));
      fail();
    }
    catch (final ServerException.LoginException ignored) {}

    try {
      server.connect(Clients.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(), "ClientTypeID",
              Collections.singletonMap(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_ID, new EmptyDomain().getDomainId())));
      fail();
    }
    catch (final ServerException.LoginException ignored) {}

    final ConnectionRequest connectionRequestTwo = Clients.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(),
            "ClientTypeID", CONNECTION_PARAMS);
    final RemoteEntityConnection remoteConnectionTwo = server.connect(connectionRequestTwo);
    admin.setLoggingEnabled(connectionRequestTwo.getClientId(), true);
    assertTrue(admin.isLoggingEnabled(connectionRequestOne.getClientId()));
    assertFalse(admin.isLoggingEnabled(UUID.randomUUID()));
    admin.setLoggingEnabled(UUID.randomUUID(), true);
    assertTrue(remoteConnectionTwo.isConnected());
    assertEquals(2, admin.getConnectionCount());
    assertEquals(2, admin.getClients().size());

    Collection<RemoteClient> clients = admin.getClients(new User(UNIT_TEST_USER.getUsername(), null));
    assertEquals(2, clients.size());
    clients = admin.getClients("ClientTypeID");
    assertEquals(2, clients.size());
    final Collection<String> clientTypes = admin.getClientTypes();
    assertEquals(1, clientTypes.size());
    assertTrue(clientTypes.contains("ClientTypeID"));

    final Collection<User> users = admin.getUsers();
    assertEquals(1, users.size());
    assertEquals(UNIT_TEST_USER, users.iterator().next());

    final EntityConditions entityConditions = new EntityConditions(remoteConnectionTwo.getDomain());
    final EntitySelectCondition selectCondition = entityConditions.selectCondition(TestDomain.T_EMP)
            .setOrderBy(Entities.orderBy().ascending(TestDomain.EMP_NAME));
    remoteConnectionTwo.selectMany(selectCondition);

    admin.getDatabaseStatistics();

    final ClientLog log = admin.getClientLog(connectionRequestTwo.getClientId());

    final MethodLogger.Entry entry = log.getEntries().get(0);
    assertEquals("getDomain", entry.getMethod());
    assertTrue(entry.getDuration() >= 0);

    admin.removeConnections(true);

    server.disconnect(connectionRequestOne.getClientId());
    assertEquals(1, admin.getConnectionCount());

    server.disconnect(connectionRequestTwo.getClientId());
    assertEquals(0, admin.getConnectionCount());

    admin.setConnectionLimit(1);
    server.connect(connectionRequestOne);
    try {
      server.connect(connectionRequestTwo);
      fail("Server should be full");
    }
    catch (final ServerException.ServerFullException ignored) {/*ignored*/}

    assertEquals(1, admin.getConnectionCount());
    admin.setConnectionLimit(2);
    server.connect(connectionRequestTwo);
    assertEquals(2, admin.getConnectionCount());

    server.disconnect(connectionRequestOne.getClientId());
    assertEquals(1, admin.getConnectionCount());
    server.disconnect(connectionRequestTwo.getClientId());
    assertEquals(0, admin.getConnectionCount());

    //testing with the TestLoginProxy
    admin.setConnectionLimit(3);
    assertEquals(3, admin.getConnectionLimit());
    final String testClientTypeId = "TestLoginProxy";
    final User john = new User("john", "hello".toCharArray());
    final ConnectionRequest connectionRequestJohn = Clients.connectionRequest(john,
            UUID.randomUUID(), testClientTypeId, CONNECTION_PARAMS);
    final ConnectionRequest connectionRequestHelen = Clients.connectionRequest(new User("helen", "juno".toCharArray()),
            UUID.randomUUID(), testClientTypeId, CONNECTION_PARAMS);
    final ConnectionRequest connectionRequestInvalid = Clients.connectionRequest(new User("foo", "bar".toCharArray()),
            UUID.randomUUID(), testClientTypeId, CONNECTION_PARAMS);
    server.connect(connectionRequestJohn);
    final RemoteClient clientJohn = admin.getClients(john).iterator().next();
    assertNotNull(clientJohn.getClientHost());
    server.connect(connectionRequestHelen);
    try {
      server.connect(connectionRequestInvalid);
      fail("Should not be able to connect with an invalid user");
    }
    catch (final ServerException.LoginException ignored) {/*ignored*/}
    final Collection<RemoteClient> empDeptClients = admin.getClients(testClientTypeId);
    assertEquals(2, empDeptClients.size());
    for (final RemoteClient empDeptClient : empDeptClients) {
      assertEquals(UNIT_TEST_USER, empDeptClient.getDatabaseUser());
    }
    server.disconnect(connectionRequestJohn.getClientId());
    assertEquals(1, admin.getConnectionCount());
    server.disconnect(connectionRequestHelen.getClientId());
    assertEquals(0, admin.getConnectionCount());
  }

  @Test
  public void remoteEntityConnectionProvider() throws Exception {
    final RemoteEntityConnectionProvider provider = (RemoteEntityConnectionProvider) new RemoteEntityConnectionProvider("TestDomain",
            UUID.randomUUID(), "TestClient").setUser(UNIT_TEST_USER);

    assertEquals(EntityConnection.Type.REMOTE, provider.getConnectionType());
    assertEquals(EntityConnection.Type.REMOTE, provider.getConnection().getType());

    assertEquals(Server.SERVER_HOST_NAME.get(), provider.getServerHostName());

    final EntityConnection db = provider.getConnection();
    assertNotNull(db);
    assertTrue(db.isConnected());
    provider.disconnect();

    final EntityConnection db2 = provider.getConnection();
    assertNotNull(db2);
    assertNotSame(db, db2);
    assertTrue(db2.isConnected());
    provider.disconnect();

    EntityConnection db3 = provider.getConnection();
    assertTrue(db3.isConnected());
    admin.disconnect(provider.getClientId());
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
  public void getClientLogNotConnected() throws RemoteException {
    assertThrows(IllegalArgumentException.class, () -> admin.getClientLog(UUID.randomUUID()));
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
    admin.setMaintenanceInterval(500);
    admin.getEntityDefinitions();
    assertEquals(500, admin.getMaintenanceInterval());
    admin.getMaxMemory();
    admin.getRequestsPerSecond();
    admin.getThreadStatistics();
    admin.getGcEvents(0);
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
    Server.SERVER_CONNECTION_SSL_ENABLED.set(true);
    DefaultEntityConnectionServer.SERVER_CONNECTION_POOLING_STARTUP_POOL_USERS.set(UNIT_TEST_USER.getUsername()
            + ":" + String.valueOf(UNIT_TEST_USER.getPassword()));
    DefaultEntityConnectionServer.SERVER_CLIENT_CONNECTION_TIMEOUT.set("ClientTypeID:10000");
    DefaultEntityConnectionServer.SERVER_DOMAIN_MODEL_CLASSES.set("org.jminor.framework.server.TestDomain");
    DefaultEntityConnectionServer.SERVER_LOGIN_PROXY_CLASSES.set("org.jminor.framework.server.TestLoginProxy");
    DefaultEntityConnectionServer.SERVER_CONNECTION_VALIDATOR_CLASSES.set("org.jminor.framework.server.TestConnectionValidator");
    DefaultEntityConnectionServer.SERVER_CLIENT_LOGGING_ENABLED.set(true);
    Server.AUXILIARY_SERVER_CLASS_NAMES.set(TestWebServer.class.getName());
    Server.RMI_SERVER_HOSTNAME.set("localhost");
    Server.TRUSTSTORE.set("src/main/security/jminor_truststore.jks");
    Server.TRUSTSTORE_PASSWORD.set("crappypass");
    Server.KEYSTORE.set("src/main/security/jminor_keystore.jks");
    Server.KEYSTORE_PASSWORD.set("crappypass");
  }

  public static class EmptyDomain extends Entities {}

  public static final class TestWebServer implements Server.AuxiliaryServer {

    public TestWebServer(final Server connectionServer) {}

    @Override
    public void startServer() throws Exception {}

    @Override
    public void stopServer() throws Exception {}
  }
}
