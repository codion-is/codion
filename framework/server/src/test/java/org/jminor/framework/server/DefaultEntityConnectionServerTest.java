/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.MethodLogger;
import org.jminor.common.User;
import org.jminor.common.db.Database;
import org.jminor.common.db.Databases;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.operation.AbstractDatabaseProcedure;
import org.jminor.common.i18n.Messages;
import org.jminor.common.remote.ClientLog;
import org.jminor.common.remote.Clients;
import org.jminor.common.remote.ConnectionRequest;
import org.jminor.common.remote.RemoteClient;
import org.jminor.common.remote.Server;
import org.jminor.common.remote.exception.ConnectionNotAvailableException;
import org.jminor.common.remote.exception.LoginException;
import org.jminor.common.remote.exception.ServerAuthenticationException;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.condition.Condition;
import org.jminor.framework.db.condition.Conditions;
import org.jminor.framework.db.condition.EntitySelectCondition;
import org.jminor.framework.db.remote.RemoteEntityConnection;
import org.jminor.framework.db.remote.RemoteEntityConnectionProvider;
import org.jminor.framework.domain.Domain;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.jminor.framework.db.condition.Conditions.entitySelectCondition;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultEntityConnectionServerTest {

  private static final User UNIT_TEST_USER =
          User.parseUser(System.getProperty("jminor.test.user", "scott:tiger"));

  private static final User ADMIN_USER = User.parseUser("scott:tiger");
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
  public void customCondition() throws Exception {
    //Fix side-effect from remoteEntityConnectionProvider() test,
    //which registeres the domain received from the server
    //thus overwriting the domain containing the custom conditions
    new TestDomain().registerDomain();
    final ConnectionRequest connectionRequestOne = Clients.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(),
            "ClientTypeID", CONNECTION_PARAMS);
    final RemoteEntityConnection connection = server.connect(connectionRequestOne);

    final Condition condition = Conditions.customCondition(TestDomain.EMP_MGR_CONDITION_ID,
            singletonList(TestDomain.EMP_MGR), singletonList(4));

    connection.select(entitySelectCondition(TestDomain.T_EMP, condition));

    connection.disconnect();
  }

  @Test
  public void remoteDomain() throws Exception {
    final ConnectionRequest connectionRequestOne = Clients.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(),
            "ClientTypeID", CONNECTION_PARAMS);
    final RemoteEntityConnection connection = server.connect(connectionRequestOne);
    final Domain domain = connection.getDomain();
    assertThrows(IllegalStateException.class, () -> domain.addOperation(new AbstractDatabaseProcedure<EntityConnection>("id", "name") {
      @Override
      public void execute(final EntityConnection connection, final Object... arguments) throws DatabaseException {}
    }));
    assertThrows(IllegalStateException.class, () -> domain.getProcedure("id"));
    assertThrows(IllegalStateException.class, () -> domain.getFunction("id"));
    connection.disconnect();
  }

  @Test
  public void testWrongPassword() throws Exception {
    assertThrows(ServerAuthenticationException.class, () -> server.connect(Clients.connectionRequest(new User(UNIT_TEST_USER.getUsername(), "foobar".toCharArray()),
            UUID.randomUUID(), getClass().getSimpleName(), CONNECTION_PARAMS)));
  }

  @Test
  public void getServerAdminEmptyPassword() throws Exception {
    assertThrows(ServerAuthenticationException.class, () -> server.getServerAdmin(new User("test", "".toCharArray())));
  }

  @Test
  public void getServerAdminNullPassword() throws Exception {
    assertThrows(ServerAuthenticationException.class, () -> server.getServerAdmin(new User("test", null)));
  }

  @Test
  public void getServerAdminWrongPassword() throws Exception {
    assertThrows(ServerAuthenticationException.class, () -> server.getServerAdmin(new User("test", "test".toCharArray())));
  }

  @Test
  public void getServerAdminEmptyUsername() throws Exception {
    assertThrows(ServerAuthenticationException.class, () -> server.getServerAdmin(new User("", "test".toCharArray())));
  }

  @Test
  public void getServerAdminWrongUsername() throws Exception {
    assertThrows(ServerAuthenticationException.class, () -> server.getServerAdmin(new User("test", "test".toCharArray())));
  }

  @Test
  public void test() throws Exception {
    final ConnectionRequest connectionRequestOne = Clients.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(),
            "ClientTypeID", CONNECTION_PARAMS);

    final RemoteEntityConnection remoteConnectionOne = server.connect(connectionRequestOne);
    assertTrue(remoteConnectionOne.isConnected());
    assertEquals(1, admin.getConnectionCount());
    admin.setPooledConnectionTimeout(UNIT_TEST_USER, 60005);
    assertEquals(60005, admin.getPooledConnectionTimeout(UNIT_TEST_USER));
    admin.setMaximumPoolCheckOutTime(UNIT_TEST_USER, 2005);
    assertEquals(2005, admin.getMaximumPoolCheckOutTime(UNIT_TEST_USER));

    try {
      server.connect(Clients.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(), "ClientTypeID"));
      fail();
    }
    catch (final LoginException ignored) {}

    try {
      server.connect(Clients.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(), "ClientTypeID",
              Collections.singletonMap(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_ID, new EmptyDomain().getDomainId())));
      fail();
    }
    catch (final LoginException ignored) {}

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

    final EntitySelectCondition selectCondition = entitySelectCondition(TestDomain.T_EMP)
            .setOrderBy(Domain.orderBy().ascending(TestDomain.EMP_NAME));
    remoteConnectionTwo.select(selectCondition);

    admin.getDatabaseStatistics();

    final ClientLog log = admin.getClientLog(connectionRequestTwo.getClientId());

    final MethodLogger.Entry entry = log.getEntries().get(0);
    assertEquals("select", entry.getMethod());
    assertTrue(entry.getDuration() >= 0);

    admin.disconnectClients(true);

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
    catch (final ConnectionNotAvailableException ignored) {/*ignored*/}

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
    catch (final LoginException ignored) {/*ignored*/}
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
    final RemoteEntityConnectionProvider provider = (RemoteEntityConnectionProvider) new RemoteEntityConnectionProvider()
            .setDomainClassName("TestDomain").setClientTypeId("TestClient").setUser(UNIT_TEST_USER);

    assertEquals(EntityConnectionProvider.CONNECTION_TYPE_REMOTE, provider.getConnectionType());

    final EntityConnection db = provider.getConnection();
    assertNotNull(db);
    assertTrue(db.isConnected());
    provider.disconnect();

    //not available until a connection has been requested
    assertEquals(Server.SERVER_HOST_NAME.get(), provider.getServerHostName());

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
    DefaultEntityConnectionServer.SERIALIZATION_FILTER_WHITELIST.set("src/main/security/serialization-whitelist.txt");
    Server.AUXILIARY_SERVER_CLASS_NAMES.set(TestWebServer.class.getName());
    Server.RMI_SERVER_HOSTNAME.set("localhost");
    Server.TRUSTSTORE.set("src/main/security/jminor_truststore.jks");
    Server.TRUSTSTORE_PASSWORD.set("crappypass");
    Server.KEYSTORE.set("src/main/security/jminor_keystore.jks");
    Server.KEYSTORE_PASSWORD.set("crappypass");
  }

  public static class EmptyDomain extends Domain {}

  public static final class TestWebServer implements Server.AuxiliaryServer {

    public TestWebServer(final Server connectionServer) {}

    @Override
    public void startServer() throws Exception {}

    @Override
    public void stopServer() throws Exception {}
  }
}
