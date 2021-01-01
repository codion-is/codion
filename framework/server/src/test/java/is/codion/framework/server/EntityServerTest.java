/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.server;

import is.codion.common.db.database.Databases;
import is.codion.common.i18n.Messages;
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
import is.codion.common.user.Users;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.Conditions;
import is.codion.framework.db.condition.SelectCondition;
import is.codion.framework.db.rmi.RemoteEntityConnection;
import is.codion.framework.db.rmi.RemoteEntityConnectionProvider;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.*;

public class EntityServerTest {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  private static final User ADMIN_USER = Users.parseUser("scott:tiger");
  private static final Map<String, Object> CONNECTION_PARAMS =
          Collections.singletonMap(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_TYPE, "TestDomain");
  private static Server<RemoteEntityConnection, EntityServerAdmin> server;
  private static EntityServerAdmin admin;

  private static final EntityServerConfiguration CONFIGURATION = configure();

  @BeforeAll
  public static synchronized void setUp() throws Exception {
    final String serverName = CONFIGURATION.getServerName();
    EntityServer.startServer(CONFIGURATION).addLoginProxy(new TestLoginProxy());
    server = (Server<RemoteEntityConnection, EntityServerAdmin>)
            LocateRegistry.getRegistry(Clients.SERVER_HOST_NAME.get(), CONFIGURATION.getRegistryPort()).lookup(serverName);
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
    //which registers the entities received from the server
    //thus overwriting the entities containing the custom conditions
    new TestDomain();
    final ConnectionRequest connectionRequestOne = ConnectionRequest.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(),
            "ClientTypeID", CONNECTION_PARAMS);
    final RemoteEntityConnection connection = server.connect(connectionRequestOne);

    final Condition condition = Conditions.customCondition(TestDomain.EMP_MGR_CONDITION_TYPE,
            singletonList(TestDomain.EMP_MGR), singletonList(4));

    connection.select(condition);

    connection.close();
  }

  @Test
  public void testWrongPassword() throws Exception {
    assertThrows(ServerAuthenticationException.class, () -> server.connect(ConnectionRequest.connectionRequest(Users.user(UNIT_TEST_USER.getUsername(), "foobar".toCharArray()),
            UUID.randomUUID(), getClass().getSimpleName(), CONNECTION_PARAMS)));
  }

  @Test
  public void getServerAdminEmptyPassword() throws Exception {
    assertThrows(ServerAuthenticationException.class, () -> server.getServerAdmin(Users.user("test", "".toCharArray())));
  }

  @Test
  public void getServerAdminNullPassword() throws Exception {
    assertThrows(ServerAuthenticationException.class, () -> server.getServerAdmin(Users.user("test")));
  }

  @Test
  public void getServerAdminWrongPassword() throws Exception {
    assertThrows(ServerAuthenticationException.class, () -> server.getServerAdmin(Users.user("test", "test".toCharArray())));
  }

  @Test
  public void getServerAdminWrongUsername() throws Exception {
    assertThrows(ServerAuthenticationException.class, () -> server.getServerAdmin(Users.user("test", "test".toCharArray())));
  }

  @Test
  public void test() throws Exception {
    final ConnectionRequest connectionRequestOne = ConnectionRequest.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(),
            "ClientTypeID", CONNECTION_PARAMS);

    final RemoteEntityConnection remoteConnectionOne = server.connect(connectionRequestOne);
    assertTrue(remoteConnectionOne.isConnected());
    assertEquals(1, admin.getConnectionCount());
    admin.setPooledConnectionTimeout(UNIT_TEST_USER.getUsername(), 60005);
    assertEquals(60005, admin.getPooledConnectionTimeout(UNIT_TEST_USER.getUsername()));
    admin.setMaximumPoolCheckOutTime(UNIT_TEST_USER.getUsername(), 2005);
    assertEquals(2005, admin.getMaximumPoolCheckOutTime(UNIT_TEST_USER.getUsername()));

    try {
      server.connect(ConnectionRequest.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(), "ClientTypeID"));
      fail();
    }
    catch (final LoginException ignored) {}

    try {
      server.connect(ConnectionRequest.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(), "ClientTypeID",
              Collections.singletonMap(RemoteEntityConnectionProvider.REMOTE_CLIENT_DOMAIN_TYPE,
                      new EmptyDomain().getDomainType().getName())));
      fail();
    }
    catch (final LoginException ignored) {}

    final ConnectionRequest connectionRequestTwo = ConnectionRequest.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(),
            "ClientTypeID", CONNECTION_PARAMS);
    final RemoteEntityConnection remoteConnectionTwo = server.connect(connectionRequestTwo);
    admin.setLoggingEnabled(connectionRequestTwo.getClientId(), true);
    assertTrue(admin.isLoggingEnabled(connectionRequestOne.getClientId()));
    assertFalse(admin.isLoggingEnabled(UUID.randomUUID()));
    admin.setLoggingEnabled(UUID.randomUUID(), true);
    assertTrue(remoteConnectionTwo.isConnected());
    assertEquals(2, admin.getConnectionCount());
    assertEquals(2, admin.getClients().size());

    Collection<RemoteClient> clients = admin.getClients(Users.user(UNIT_TEST_USER.getUsername()));
    assertEquals(2, clients.size());
    clients = admin.getClients("ClientTypeID");
    assertEquals(2, clients.size());
    final Collection<String> clientTypes = admin.getClientTypes();
    assertEquals(1, clientTypes.size());
    assertTrue(clientTypes.contains("ClientTypeID"));

    final Collection<User> users = admin.getUsers();
    assertEquals(1, users.size());
    assertEquals(UNIT_TEST_USER, users.iterator().next());

    final SelectCondition selectCondition = Conditions.condition(TestDomain.T_EMP).select()
            .orderBy(orderBy().ascending(TestDomain.EMP_NAME));
    remoteConnectionTwo.select(selectCondition);

    admin.getDatabaseStatistics();

    final ClientLog log = admin.getClientLog(connectionRequestTwo.getClientId());

    final MethodLogger.Entry entry = log.getEntries().get(0);
    assertEquals("select", entry.getMethod());
    assertTrue(entry.getDuration() >= 0);

    admin.disconnectTimedOutClients();

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
    final User john = Users.user("john", "hello".toCharArray());
    final ConnectionRequest connectionRequestJohn = ConnectionRequest.connectionRequest(john,
            UUID.randomUUID(), testClientTypeId, CONNECTION_PARAMS);
    final ConnectionRequest connectionRequestHelen = ConnectionRequest.connectionRequest(Users.user("helen", "juno".toCharArray()),
            UUID.randomUUID(), testClientTypeId, CONNECTION_PARAMS);
    final ConnectionRequest connectionRequestInvalid = ConnectionRequest.connectionRequest(Users.user("foo", "bar".toCharArray()),
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
    final RemoteEntityConnectionProvider provider = (RemoteEntityConnectionProvider)
            new RemoteEntityConnectionProvider("localhost", CONFIGURATION.getServerPort(), CONFIGURATION.getRegistryPort())
                    .setDomainClassName("TestDomain").setClientTypeId("TestClient").setUser(UNIT_TEST_USER);

    assertEquals(EntityConnectionProvider.CONNECTION_TYPE_REMOTE, provider.getConnectionType());

    final EntityConnection db = provider.getConnection();
    assertNotNull(db);
    assertTrue(db.isConnected());
    provider.close();

    //not available until a connection has been requested
    assertEquals(Clients.SERVER_HOST_NAME.get(), provider.getServerHostName());

    final EntityConnection db2 = provider.getConnection();
    assertNotNull(db2);
    assertNotSame(db, db2);
    assertTrue(db2.isConnected());
    provider.close();

    EntityConnection db3 = provider.getConnection();
    assertTrue(db3.isConnected());
    admin.disconnect(provider.getClientId());
    assertFalse(db3.isConnected());

    db3 = provider.getConnection();
    assertTrue(db3.isConnected());
    db3.close();

    provider.close();
    assertEquals("localhost" + " - " + Messages.get(Messages.NOT_CONNECTED), provider.getDescription());
    db3 = provider.getConnection();
    assertEquals(admin.getServerInformation().getServerName() + "@localhost", provider.getDescription());
    db3.close();
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
    admin.getDatabaseUrl();
    admin.getConnectionPoolUsernames();
    admin.setMaintenanceInterval(500);
    admin.getEntityDefinitions();
    assertEquals(500, admin.getMaintenanceInterval());
    admin.getMaxMemory();
    admin.getRequestsPerSecond();
    admin.getThreadStatistics();
    admin.getGcEvents(0);
    admin.getServerInformation();
    admin.getSystemProperties();
    admin.getUsedMemory();
    admin.getUsers();
    admin.getServerStatistics(System.currentTimeMillis());
  }

  private static DefaultEntityServerConfiguration configure() {
    Clients.SERVER_HOST_NAME.set("localhost");
    Clients.TRUSTSTORE.set("src/main/security/truststore.jks");
    Clients.TRUSTSTORE_PASSWORD.set("crappypass");
    ServerConfiguration.RMI_SERVER_HOSTNAME.set("localhost");
    ServerConfiguration.KEYSTORE.set("src/main/security/keystore.jks");
    ServerConfiguration.KEYSTORE_PASSWORD.set("crappypass");
    final DefaultEntityServerConfiguration configuration = new DefaultEntityServerConfiguration(3223, 3221);
    configuration.setServerAdminPort(3223);
    configuration.setAdminUser(Users.parseUser("scott:tiger"));
    configuration.setDatabase(Databases.getInstance());
    configuration.setStartupPoolUsers(singletonList(UNIT_TEST_USER));
    configuration.setClientSpecificConnectionTimeouts(singletonMap("ClientTypeID", 10000));
    configuration.setDomainModelClassNames(singletonList("is.codion.framework.server.TestDomain"));
    configuration.setClientLoggingEnabled(true);
    configuration.setSslEnabled(true);
    configuration.setSerializationFilterWhitelist("src/test/security/serialization-whitelist-test.txt");

    return configuration;
  }

  public static class EmptyDomain extends DefaultDomain {
    private EmptyDomain() {
      super(DomainType.domainType(EmptyDomain.class));
    }
  }
}
