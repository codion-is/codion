/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

public class DefaultEntityConnectionServerTest {

  private static final Entities ENTITIES = new TestDomain();

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger"));

  private static final int WEB_SERVER_PORT_NUMBER = 8089;

  private static final User ADMIN_USER = new User("scott", "tiger");
  private static final Map<String, Object> CONNECTION_PARAMS = Collections.singletonMap("jminor.client.domainModelClass", TestDomain.class);
  private static Server<RemoteEntityConnection, EntityConnectionServerAdmin> server;
  private static EntityConnectionServerAdmin admin;

  public static EntityConnectionServerAdmin getServerAdmin() {
    return admin;
  }

  @BeforeClass
  public static synchronized void setUp() throws Exception {
    configure();
    final Database database = Databases.getInstance();
    final String serverName = DefaultEntityConnectionServer.initializeServerName(database.getHost(), database.getSid());
    DefaultEntityConnectionServer.startServer();
    server = (Server) LocateRegistry.getRegistry(Server.SERVER_HOST_NAME.get(), Server.REGISTRY_PORT.get()).lookup(serverName);
    admin = server.getServerAdmin(ADMIN_USER);
  }

  @AfterClass
  public static synchronized void tearDown() throws Exception {
    admin.shutdown();
    server = null;
  }

  @Test(expected = ServerException.AuthenticationException.class)
  public void testWrongPassword() throws Exception {
    server.connect(Clients.connectionRequest(new User(UNIT_TEST_USER.getUsername(), "foobar"),
            UUID.randomUUID(), getClass().getSimpleName(), CONNECTION_PARAMS));
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
    catch (final ServerException.LoginException e) {}

    try {
      server.connect(Clients.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(), "ClientTypeID",
              Collections.singletonMap("jminor.client.domainModelClass", EmptyDomain.class)));
      fail();
    }
    catch (final ServerException.LoginException e) {}

    final ConnectionRequest connectionRequestTwo = Clients.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(),
            "ClientTypeID", CONNECTION_PARAMS);
    final RemoteEntityConnection remoteConnectionTwo = server.connect(connectionRequestTwo);
    admin.setLoggingEnabled(connectionRequestTwo.getClientID(), true);
    assertTrue(admin.isLoggingEnabled(connectionRequestOne.getClientID()));
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

    final EntityConditions entityConditions = new EntityConditions(new TestDomain());
    final EntitySelectCondition selectCondition = entityConditions.selectCondition(TestDomain.T_EMP)
            .orderByAscending(TestDomain.EMP_NAME);
    remoteConnectionTwo.selectMany(selectCondition);

    final Database.Statistics stats = admin.getDatabaseStatistics();

    final ClientLog log = admin.getClientLog(connectionRequestTwo.getClientID());

    final MethodLogger.Entry entry = log.getEntries().get(0);
    assertEquals("selectMany", entry.getMethod());
    assertTrue(entry.getDuration() >= 0);

    admin.removeConnections(true);

    server.disconnect(connectionRequestOne.getClientID());
    assertEquals(1, admin.getConnectionCount());

    server.disconnect(connectionRequestTwo.getClientID());
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

    server.disconnect(connectionRequestOne.getClientID());
    assertEquals(1, admin.getConnectionCount());
    server.disconnect(connectionRequestTwo.getClientID());
    assertEquals(0, admin.getConnectionCount());

    //testing with the TestLoginProxy
    admin.setConnectionLimit(3);
    assertEquals(3, admin.getConnectionLimit());
    final String testClientTypeID = "TestLoginProxy";
    final ConnectionRequest connectionRequestJohn = Clients.connectionRequest(new User("john", "hello"),
            UUID.randomUUID(), testClientTypeID, CONNECTION_PARAMS);
    final ConnectionRequest connectionRequestHelen = Clients.connectionRequest(new User("helen", "juno"),
            UUID.randomUUID(), testClientTypeID, CONNECTION_PARAMS);
    final ConnectionRequest connectionRequestInvalid = Clients.connectionRequest(new User("foo", "bar"),
            UUID.randomUUID(), testClientTypeID, CONNECTION_PARAMS);
    server.connect(connectionRequestJohn);
    server.connect(connectionRequestHelen);
    try {
      server.connect(connectionRequestInvalid);
      fail("Should not be able to connect with an invalid user");
    }
    catch (final ServerException.LoginException ignored) {/*ignored*/}
    final Collection<RemoteClient> empDeptClients = admin.getClients(testClientTypeID);
    assertEquals(2, empDeptClients.size());
    for (final RemoteClient empDeptClient : empDeptClients) {
      assertEquals(UNIT_TEST_USER, empDeptClient.getDatabaseUser());
    }
    server.disconnect(connectionRequestJohn.getClientID());
    assertEquals(1, admin.getConnectionCount());
    server.disconnect(connectionRequestHelen.getClientID());
    assertEquals(0, admin.getConnectionCount());
  }

  @Test
  public void remoteEntityConnectionProvider() throws Exception {
    final RemoteEntityConnectionProvider provider = new RemoteEntityConnectionProvider(ENTITIES, "localhost",
            UNIT_TEST_USER, UUID.randomUUID(), "TestClient");

    assertEquals(EntityConnection.Type.REMOTE, provider.getConnectionType());
    assertEquals(EntityConnection.Type.REMOTE, provider.getConnection().getType());

    assertEquals(Server.SERVER_HOST_NAME.get(), provider.getServerHostName());

    final EntityConnection db = provider.getConnection();
    assertNotNull(db);
    assertTrue(db.isConnected());
    provider.disconnect();

    final EntityConnection db2 = provider.getConnection();
    assertNotNull(db2);
    assertFalse(db == db2);
    assertTrue(db2.isConnected());
    provider.disconnect();

    EntityConnection db3 = provider.getConnection();
    assertTrue(db3.isConnected());
    admin.disconnect(provider.getClientID());
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

  @Test(expected = IllegalArgumentException.class)
  public void getClientLogNotConnected() throws RemoteException {
    admin.getClientLog(UUID.randomUUID());
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
    Server.TRUSTSTORE.set("../../resources/security/JMinorClientTruststore");
    System.setProperty("javax.net.ssl.keyStore", "../../resources/security/JMinorServerKeystore");
    System.setProperty("javax.net.ssl.keyStorePassword", "crappypass");
  }

  public static class EmptyDomain extends Entities {}

  public static final class TestWebServer implements Server.AuxiliaryServer {

    public TestWebServer(final Entities entities, final Server connectionServer, final String documentRoot, final Integer port) {}

    @Override
    public void startServer() throws Exception {}

    @Override
    public void stopServer() throws Exception {}
  }
}
