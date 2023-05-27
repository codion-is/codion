/*
 * Copyright (c) 2011 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.server;

import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.rmi.server.ServerAdmin.ServerStatistics;
import is.codion.common.rmi.server.ServerAdmin.ThreadStatistics;
import is.codion.common.rmi.server.exception.ConnectionNotAvailableException;
import is.codion.common.rmi.server.exception.ServerAuthenticationException;
import is.codion.common.rmi.server.exception.ServerException;
import is.codion.common.user.User;

import org.junit.jupiter.api.Test;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class AbstractServerTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));
  public static final int PORT = 1234;

  @Test
  void testConnectionCount() throws RemoteException, ServerException {
    TestServer server = new TestServer();
    String clientTypeId = "clientTypeId";
    ConnectionRequest connectionRequest = ConnectionRequest.builder().user(UNIT_TEST_USER).clientTypeId(clientTypeId).build();
    ConnectionRequest connectionRequest2 = ConnectionRequest.builder().user(UNIT_TEST_USER).clientTypeId(clientTypeId).build();
    ConnectionRequest connectionRequest3 = ConnectionRequest.builder().user(UNIT_TEST_USER).clientTypeId(clientTypeId).build();
    server.connect(connectionRequest);
    assertEquals(1, server.connectionCount());
    server.connect(connectionRequest2);
    assertEquals(2, server.connectionCount());
    server.disconnect(connectionRequest.clientId());
    assertEquals(1, server.connectionCount());
    server.connect(connectionRequest);
    assertEquals(2, server.connectionCount());
    server.connect(connectionRequest3);
    assertEquals(3, server.connectionCount());
    server.disconnect(connectionRequest3.clientId());
    assertEquals(2, server.connectionCount());
    server.disconnect(connectionRequest2.clientId());
    assertEquals(1, server.connectionCount());
    server.disconnect(connectionRequest.clientId());
    assertEquals(0, server.connectionCount());
    server.shutdown();
  }

  @Test
  void testConnectionLimitReached() throws RemoteException, ServerException {
    TestServer server = new TestServer();
    String clientTypeId = "clientTypeId";
    ConnectionRequest connectionRequest = ConnectionRequest.builder().user(UNIT_TEST_USER).clientTypeId(clientTypeId).build();
    ConnectionRequest connectionRequest2 = ConnectionRequest.builder().user(UNIT_TEST_USER).clientTypeId(clientTypeId).build();
    server.setConnectionLimit(1);
    assertEquals(1, server.getConnectionLimit());
    server.connect(connectionRequest);
    assertThrows(ConnectionNotAvailableException.class, () -> server.connect(connectionRequest2));
    server.shutdown();
  }

  @Test
  void testConnect() throws RemoteException, ServerException {
    TestServer server = new TestServer();
    String clientTypeId = "clientTypeId";
    ConnectionRequest connectionRequest = ConnectionRequest.builder().user(UNIT_TEST_USER).clientTypeId(clientTypeId).build();
    ServerTest connection = server.connect(connectionRequest);
    assertNotNull(connection);
    ServerTest connection2 = server.connect(connectionRequest);
    assertSame(connection, connection2);
    Map<RemoteClient, ServerTest> connections = server.connections();
    assertEquals(1, connections.size());
    assertEquals(connection, connections.get(connectionRequest));
    assertEquals(connection, server.connection(connectionRequest.clientId()));
    assertNotNull(server.connection(connectionRequest.clientId()));

    ServerAdmin admin = server.getAdmin();
    Collection<RemoteClient> clients = admin.clients();
    assertFalse(clients.isEmpty());
    clients.forEach(client -> assertEquals(0, client.user().password().length));
    clients.forEach(client -> assertEquals(0, client.databaseUser().password().length));
    admin.users().forEach(user -> assertEquals(0, user.password().length));

    RemoteClient client = server.clients(UNIT_TEST_USER).iterator().next();
    client.connectionRequest();
    client.getClientHost();
    client.clientVersion();
    client.frameworkVersion();
    client.databaseUser();
    client.toString();
    server.disconnect(connectionRequest.clientId());
    assertThrows(IllegalArgumentException.class, () -> server.connection(connectionRequest.clientId()));
    ServerTest connection3 = server.connect(connectionRequest);
    assertNotSame(connection, connection3);
    assertNotNull(server.serverInformation());
    admin.disconnect(connection3.remoteClient().clientId());
    assertThrows(IllegalArgumentException.class, () -> server.connection(connection3.remoteClient().clientId()));
    assertThrows(NullPointerException.class, () -> server.connect((ConnectionRequest) null));
    server.shutdown();
  }

  @Test
  void testLoginProxy() throws RemoteException, ServerException {
    TestLoginProxy.LOGIN_COUNTER.set(0);
    TestLoginProxy.LOGOUT_COUNTER.set(0);
    TestLoginProxy.CLOSE_COUNTER.set(0);

    String clientTypeId = "clientTypeId";
    ServerConfiguration configuration = configuration();
    TestServer server = new TestServer(configuration);
    ConnectionRequest connectionRequest = ConnectionRequest.builder().user(UNIT_TEST_USER).clientTypeId(clientTypeId).build();
    ServerTest connection = server.connect(connectionRequest);
    assertNotNull(connection);
    assertEquals(connectionRequest.clientId(), connection.remoteClient().clientId());

    server.disconnect(connectionRequest.clientId());

    connection = server.connect(connectionRequest);
    assertEquals(2, TestLoginProxy.LOGIN_COUNTER.get());
    assertNotNull(connection);
    assertEquals(connectionRequest.clientId(), connection.remoteClient().clientId());

    server.disconnect(connectionRequest.clientId());
    assertEquals(2, TestLoginProxy.LOGOUT_COUNTER.get());

    connection = server.connect(connectionRequest);
    assertEquals(3, TestLoginProxy.LOGIN_COUNTER.get());
    assertNotNull(connection);
    assertEquals(connectionRequest.clientId(), connection.remoteClient().clientId());

    server.shutdown();
    assertEquals(3, TestLoginProxy.LOGOUT_COUNTER.get());
    assertEquals(1, TestLoginProxy.CLOSE_COUNTER.get());
  }

  @Test
  void connectionTheftWrongPassword() throws RemoteException, ServerException {
    TestServer server = new TestServer();
    final String clientTypeId = "clientTypeId";

    UUID connectionId = UUID.randomUUID();
    ConnectionRequest connectionRequest = ConnectionRequest.builder()
            .user(UNIT_TEST_USER).clientId(connectionId).clientTypeId(clientTypeId).build();
    ConnectionRequest connectionRequest2 = ConnectionRequest.builder()
            .user(User.user(UNIT_TEST_USER.username(), "test".toCharArray())).clientId(connectionId).clientTypeId(clientTypeId).build();

    server.connect(connectionRequest);

    //try to steal the connection using the same connectionId, but incorrect user credentials
    assertThrows(ServerAuthenticationException.class, () -> server.connect(connectionRequest2));

    server.shutdown();
  }

  @Test
  void connectionTheftWrongUsername() throws RemoteException, ServerException {
    TestServer server = new TestServer();
    final String clientTypeId = "clientTypeId";

    UUID connectionId = UUID.randomUUID();
    ConnectionRequest connectionRequest = ConnectionRequest.builder()
            .user(UNIT_TEST_USER).clientId(connectionId).clientTypeId(clientTypeId).build();
    ConnectionRequest connectionRequest2 = ConnectionRequest.builder()
            .user(User.user("test", UNIT_TEST_USER.password()))
            .clientId(connectionId)
            .clientTypeId(clientTypeId)
            .build();

    server.connect(connectionRequest);

    //try to steal the connection using the same connectionId, but incorrect user credentials
    assertThrows(ServerAuthenticationException.class, () -> server.connect(connectionRequest2));

    server.shutdown();
  }

  @Test
  void admin() throws RemoteException {
    TestServer server = new TestServer();
    ServerAdmin admin = server.getAdmin();
    admin.clients();
    admin.clientTypes();
    admin.connectionCount();
    admin.setConnectionLimit(10);
    admin.getConnectionLimit();
    admin.maxMemory();
    admin.allocatedMemory();
    admin.clients("test");
    admin.users();
    admin.clients(User.user("test"));
    admin.systemProperties();
    try {
      admin.threadStatistics();
    }
    catch (NullPointerException e) {/*Intermittent failure when run in Github actions*/}
    admin.gcEvents(System.currentTimeMillis());
    admin.requestsPerSecond();
    admin.systemCpuLoad();
    admin.processCpuLoad();
    ServerInformation serverInformation = admin.serverInformation();
    serverInformation.serverName();
    serverInformation.serverId();
    serverInformation.serverPort();
    serverInformation.serverVersion();
    serverInformation.locale();
    serverInformation.timeZone();
    serverInformation.startTime();
    ServerStatistics serverStatistics = admin.serverStatistics(System.currentTimeMillis());
    serverStatistics.connectionCount();
    serverStatistics.connectionLimit();
    serverStatistics.usedMemory();
    try {
      ThreadStatistics threadStatistics = serverStatistics.threadStatistics();
      threadStatistics.threadCount();
      threadStatistics.daemonThreadCount();
      threadStatistics.threadStateCount();
    }
    catch (NullPointerException e) {/*See above*/}
    serverStatistics.allocatedMemory();
    serverStatistics.gcEvents();
    serverStatistics.connectionCount();
    serverStatistics.maximumMemory();
    serverStatistics.connectionLimit();
    serverStatistics.processCpuLoad();
    serverStatistics.systemCpuLoad();
    serverStatistics.requestsPerSecond();
    serverStatistics.timestamp();
    admin.shutdown();
  }

  private static class ServerTestImpl implements ServerTest {

    private final RemoteClient remoteClient;

    public ServerTestImpl(RemoteClient remoteClient) {
      this.remoteClient = remoteClient;
    }

    @Override
    public RemoteClient remoteClient() throws RemoteException {
      return remoteClient;
    }
  }

  private interface ServerTest extends Remote {
    RemoteClient remoteClient() throws RemoteException;
  }

  private static ServerConfiguration configuration() {
    return ServerConfiguration.builder(PORT).serverName("remoteServerTestServer").build();
  }

  private static final class TestServer extends AbstractServer<ServerTest, ServerAdmin> {

    private static final ServerConfiguration CONFIGURATION = configuration();

    private TestServer() throws RemoteException {
      this(CONFIGURATION);
      setAdmin(new DefaultServerAdmin(this, CONFIGURATION));
    }

    private TestServer(ServerConfiguration configuration) throws RemoteException {
      super(configuration);
      addLoginProxy(new TestLoginProxy());
    }

    @Override
    protected ServerTest connect(RemoteClient remoteClient) {
      return new ServerTestImpl(remoteClient);
    }

    @Override
    public ServerAdmin serverAdmin(User user) throws RemoteException, ServerAuthenticationException {
      return getAdmin();
    }

    @Override
    protected void disconnect(ServerTest connection) {}

    @Override
    protected void maintainConnections(Collection<ClientConnection<ServerTest>> connections) throws RemoteException {}

    @Override
    public int serverLoad() {
      return 0;
    }
  }

  public static final class TestLoginProxy implements LoginProxy {

    static final AtomicInteger LOGIN_COUNTER = new AtomicInteger();
    static final AtomicInteger LOGOUT_COUNTER = new AtomicInteger();
    static final AtomicInteger CLOSE_COUNTER = new AtomicInteger();

    @Override
    public String clientTypeId() {
      return null;
    }
    @Override
    public RemoteClient login(RemoteClient remoteClient) {
      LOGIN_COUNTER.incrementAndGet();
      return remoteClient;
    }
    @Override
    public void logout(RemoteClient remoteClient) {
      LOGOUT_COUNTER.incrementAndGet();
    }
    @Override
    public void close() {
      CLOSE_COUNTER.incrementAndGet();
    }
  }
}
