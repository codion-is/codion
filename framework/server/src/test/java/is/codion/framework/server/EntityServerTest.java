/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.server;

import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.logging.MethodLogger;
import is.codion.common.rmi.client.Clients;
import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.rmi.server.ClientLog;
import is.codion.common.rmi.server.RemoteClient;
import is.codion.common.rmi.server.Server;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.rmi.server.exception.ConnectionNotAvailableException;
import is.codion.common.rmi.server.exception.LoginException;
import is.codion.common.rmi.server.exception.ServerAuthenticationException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.Conditions;
import is.codion.framework.db.condition.SelectCondition;
import is.codion.framework.db.rmi.RemoteEntityConnection;
import is.codion.framework.db.rmi.RemoteEntityConnectionProvider;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.OrderBy;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Collection;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.*;

public class EntityServerTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final User ADMIN_USER = User.parse("scott:tiger");
  private static Server<RemoteEntityConnection, EntityServerAdmin> server;
  private static EntityServerAdmin admin;

  private static final EntityServerConfiguration CONFIGURATION = configure();

  @BeforeAll
  public static synchronized void setUp() throws Exception {
    String serverName = CONFIGURATION.serverName();
    EntityServer.startServer(CONFIGURATION).addLoginProxy(new TestLoginProxy());
    server = (Server<RemoteEntityConnection, EntityServerAdmin>)
            LocateRegistry.getRegistry(Clients.SERVER_HOST_NAME.get(), CONFIGURATION.registryPort()).lookup(serverName);
    admin = server.getServerAdmin(ADMIN_USER);
  }

  @AfterAll
  public static synchronized void tearDown() throws Exception {
    admin.shutdown();
    server = null;
  }

  @Test
  void customCondition() throws Exception {
    //Fix side-effect from remoteEntityConnectionProvider() test,
    //which registers the entities received from the server
    //thus overwriting the entities containing the custom conditions
    new TestDomain();
    ConnectionRequest connectionRequestOne = ConnectionRequest.builder()
            .user(UNIT_TEST_USER)
            .clientTypeId("ClientTypeID")
            .parameter(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_TYPE, "TestDomain").build();
    RemoteEntityConnection connection = server.connect(connectionRequestOne);

    Condition condition = Conditions.customCondition(TestDomain.EMP_MGR_CONDITION_TYPE,
            singletonList(TestDomain.EMP_MGR), singletonList(4));

    connection.select(condition);

    connection.close();
  }

  @Test
  void testWrongPassword() throws Exception {
    assertThrows(ServerAuthenticationException.class, () -> server.connect(ConnectionRequest.builder()
            .user(User.user(UNIT_TEST_USER.getUsername(), "foobar".toCharArray()))
            .clientTypeId(getClass().getSimpleName())
            .parameter(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_TYPE, "TestDomain").build()));
  }

  @Test
  void getServerAdminEmptyPassword() throws Exception {
    assertThrows(ServerAuthenticationException.class, () -> server.getServerAdmin(User.user("test", "".toCharArray())));
  }

  @Test
  void getServerAdminNullPassword() throws Exception {
    assertThrows(ServerAuthenticationException.class, () -> server.getServerAdmin(User.user("test")));
  }

  @Test
  void getServerAdminWrongPassword() throws Exception {
    assertThrows(ServerAuthenticationException.class, () -> server.getServerAdmin(User.user("test", "test".toCharArray())));
  }

  @Test
  void getServerAdminWrongUsername() throws Exception {
    assertThrows(ServerAuthenticationException.class, () -> server.getServerAdmin(User.user("test", "test".toCharArray())));
  }

  @Test
  void test() throws Exception {
    ConnectionRequest connectionRequestOne = ConnectionRequest.builder()
            .user(UNIT_TEST_USER)
            .clientTypeId("ClientTypeID").parameter(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_TYPE, "TestDomain").build();

    RemoteEntityConnection remoteConnectionOne = server.connect(connectionRequestOne);
    assertTrue(remoteConnectionOne.isConnected());
    assertEquals(1, admin.getConnectionCount());
    admin.setPooledConnectionIdleTimeout(UNIT_TEST_USER.getUsername(), 60005);
    assertEquals(60005, admin.getPooledConnectionIdleTimeout(UNIT_TEST_USER.getUsername()));
    admin.setMaximumPoolCheckOutTime(UNIT_TEST_USER.getUsername(), 2005);
    assertEquals(2005, admin.getMaximumPoolCheckOutTime(UNIT_TEST_USER.getUsername()));

    try {
      server.connect(ConnectionRequest.builder().user(UNIT_TEST_USER).clientTypeId("ClientTypeID").build());
      fail();
    }
    catch (LoginException ignored) {}

    try {
      server.connect(ConnectionRequest.builder()
              .user(UNIT_TEST_USER)
              .clientTypeId("ClientTypeID")
              .parameter(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_TYPE,
                      new EmptyDomain().type().name()).build());
      fail();
    }
    catch (LoginException ignored) {}

    ConnectionRequest connectionRequestTwo = ConnectionRequest.builder()
            .user(UNIT_TEST_USER)
            .clientTypeId("ClientTypeID")
            .parameter(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_TYPE, "TestDomain").build();
    RemoteEntityConnection remoteConnectionTwo = server.connect(connectionRequestTwo);
    admin.setLoggingEnabled(connectionRequestTwo.clientId(), true);
    assertTrue(admin.isLoggingEnabled(connectionRequestOne.clientId()));
    assertThrows(IllegalArgumentException.class, () -> admin.isLoggingEnabled(UUID.randomUUID()));
    assertThrows(IllegalArgumentException.class, () -> admin.setLoggingEnabled(UUID.randomUUID(), true));
    assertTrue(remoteConnectionTwo.isConnected());
    assertEquals(2, admin.getConnectionCount());
    assertEquals(2, admin.getClients().size());

    Collection<RemoteClient> clients = admin.getClients(User.user(UNIT_TEST_USER.getUsername()));
    assertEquals(2, clients.size());
    clients = admin.getClients("ClientTypeID");
    assertEquals(2, clients.size());
    Collection<String> clientTypes = admin.getClientTypes();
    assertEquals(1, clientTypes.size());
    assertTrue(clientTypes.contains("ClientTypeID"));

    Collection<User> users = admin.getUsers();
    assertEquals(1, users.size());
    assertEquals(UNIT_TEST_USER, users.iterator().next());

    SelectCondition selectCondition = Conditions.condition(TestDomain.T_EMP)
            .selectBuilder()
            .orderBy(OrderBy.ascending(TestDomain.EMP_NAME))
            .build();
    remoteConnectionTwo.select(selectCondition);

    admin.getDatabaseStatistics();

    ClientLog log = admin.getClientLog(connectionRequestTwo.clientId());

    MethodLogger.Entry entry = log.getEntries().get(0);
    assertEquals("select", entry.getMethod());
    assertTrue(entry.getDuration() >= 0);

    admin.disconnectTimedOutClients();

    server.disconnect(connectionRequestOne.clientId());
    assertEquals(1, admin.getConnectionCount());

    server.disconnect(connectionRequestTwo.clientId());
    assertEquals(0, admin.getConnectionCount());

    admin.setConnectionLimit(1);
    server.connect(connectionRequestOne);
    try {
      server.connect(connectionRequestTwo);
      fail("Server should be full");
    }
    catch (ConnectionNotAvailableException ignored) {/*ignored*/}

    assertEquals(1, admin.getConnectionCount());
    admin.setConnectionLimit(2);
    server.connect(connectionRequestTwo);
    assertEquals(2, admin.getConnectionCount());

    server.disconnect(connectionRequestOne.clientId());
    assertEquals(1, admin.getConnectionCount());
    server.disconnect(connectionRequestTwo.clientId());
    assertEquals(0, admin.getConnectionCount());

    //testing with the TestLoginProxy
    admin.setConnectionLimit(3);
    assertEquals(3, admin.getConnectionLimit());
    final String testClientTypeId = "TestLoginProxy";
    User john = User.parse("john:hello");
    ConnectionRequest connectionRequestJohn = ConnectionRequest.builder()
            .user(john)
            .clientTypeId(testClientTypeId)
            .parameter(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_TYPE, "TestDomain").build();
    ConnectionRequest connectionRequestHelen = ConnectionRequest.builder()
            .user(User.parse("helen:juno"))
            .clientTypeId(testClientTypeId)
            .parameter(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_TYPE, "TestDomain").build();
    ConnectionRequest connectionRequestInvalid = ConnectionRequest.builder()
            .user(User.parse("foo:bar"))
            .clientTypeId(testClientTypeId)
            .parameter(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_TYPE, "TestDomain").build();
    server.connect(connectionRequestJohn);
    RemoteClient clientJohn = admin.getClients(john).iterator().next();
    assertNotNull(clientJohn.getClientHost());
    server.connect(connectionRequestHelen);
    try {
      server.connect(connectionRequestInvalid);
      fail("Should not be able to connect with an invalid user");
    }
    catch (LoginException ignored) {/*ignored*/}
    Collection<RemoteClient> empDeptClients = admin.getClients(testClientTypeId);
    assertEquals(2, empDeptClients.size());
    for (RemoteClient empDeptClient : empDeptClients) {
      assertEquals(UNIT_TEST_USER, empDeptClient.databaseUser());
    }
    server.disconnect(connectionRequestJohn.clientId());
    assertEquals(1, admin.getConnectionCount());
    server.disconnect(connectionRequestHelen.clientId());
    assertEquals(0, admin.getConnectionCount());
  }

  @Test
  void remoteEntityConnectionProvider() throws Exception {
    RemoteEntityConnectionProvider provider =
            RemoteEntityConnectionProvider.builder()
                    .serverHostName("localhost")
                    .serverPort(CONFIGURATION.serverPort())
                    .registryPort(CONFIGURATION.registryPort())
                    .domainClassName("TestDomain")
                    .clientTypeId("TestClient")
                    .user(UNIT_TEST_USER)
                    .build();

    assertEquals(EntityConnectionProvider.CONNECTION_TYPE_REMOTE, provider.connectionType());

    EntityConnection db = provider.connection();
    assertNotNull(db);
    assertTrue(db.isConnected());
    provider.close();

    //not available until a connection has been requested
    assertEquals(Clients.SERVER_HOST_NAME.get(), provider.getServerHostName());

    EntityConnection db2 = provider.connection();
    assertNotNull(db2);
    assertNotSame(db, db2);
    assertTrue(db2.isConnected());
    provider.close();

    EntityConnection db3 = provider.connection();
    assertTrue(db3.isConnected());
    admin.disconnect(provider.clientId());
    assertFalse(db3.isConnected());

    db3 = provider.connection();
    assertTrue(db3.isConnected());
    db3.close();

    provider.close();
    assertFalse(provider.isConnectionValid());
    db3 = provider.connection();
    assertTrue(provider.isConnectionValid());
    db3.close();
  }

  @Test
  void getClientLogNotConnected() throws RemoteException {
    assertThrows(IllegalArgumentException.class, () -> admin.getClientLog(UUID.randomUUID()));
  }

  private static EntityServerConfiguration configure() {
    Clients.SERVER_HOST_NAME.set("localhost");
    Clients.TRUSTSTORE.set("src/main/config/truststore.jks");
    Clients.TRUSTSTORE_PASSWORD.set("crappypass");
    Clients.resolveTrustStore();
    ServerConfiguration.RMI_SERVER_HOSTNAME.set("localhost");
    ServerConfiguration.KEYSTORE.set("src/main/config/keystore.jks");
    ServerConfiguration.KEYSTORE_PASSWORD.set("crappypass");

    return EntityServerConfiguration.builder(3223, 3221)
            .adminPort(3223)
            .adminUser(User.parse("scott:tiger"))
            .database(DatabaseFactory.getDatabase())
            .connectionPoolUsers(singletonList(UNIT_TEST_USER))
            .clientTypeIdleConnectionTimeouts(singletonMap("ClientTypeID", 10000))
            .domainModelClassNames(singletonList("is.codion.framework.server.TestDomain"))
            .clientLoggingEnabled(true)
            .sslEnabled(true)
            .serializationFilterWhitelist("classpath:serialization-whitelist-test.txt")
            .build();
  }

  public static class EmptyDomain extends DefaultDomain {
    private EmptyDomain() {
      super(DomainType.domainType(EmptyDomain.class));
    }
  }
}
