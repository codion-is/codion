/*
 * Copyright (c) 2011 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
    assertEquals(1, server.getConnectionCount());
    server.connect(connectionRequest2);
    assertEquals(2, server.getConnectionCount());
    server.disconnect(connectionRequest.getClientId());
    assertEquals(1, server.getConnectionCount());
    server.connect(connectionRequest);
    assertEquals(2, server.getConnectionCount());
    server.connect(connectionRequest3);
    assertEquals(3, server.getConnectionCount());
    server.disconnect(connectionRequest3.getClientId());
    assertEquals(2, server.getConnectionCount());
    server.disconnect(connectionRequest2.getClientId());
    assertEquals(1, server.getConnectionCount());
    server.disconnect(connectionRequest.getClientId());
    assertEquals(0, server.getConnectionCount());
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
    Map<RemoteClient, ServerTest> connections = server.getConnections();
    assertEquals(1, connections.size());
    assertEquals(connection, connections.get(connectionRequest));
    assertEquals(connection, server.getConnection(connectionRequest.getClientId()));
    assertNotNull(server.getConnection(connectionRequest.getClientId()));
    RemoteClient client = server.getClients(UNIT_TEST_USER).iterator().next();
    client.getConnectionRequest();
    client.getClientHost();
    client.getClientVersion();
    client.getFrameworkVersion();
    client.getDatabaseUser();
    client.toString();
    server.disconnect(connectionRequest.getClientId());
    assertThrows(IllegalArgumentException.class, () -> server.getConnection(connectionRequest.getClientId()));
    ServerTest connection3 = server.connect(connectionRequest);
    assertNotSame(connection, connection3);
    assertNotNull(server.getServerInformation());
    server.getAdmin().disconnect(connection3.getRemoteClient().getClientId());
    assertThrows(IllegalArgumentException.class, () -> server.getConnection(connection3.getRemoteClient().getClientId()));
    assertThrows(NullPointerException.class, () -> server.connect((ConnectionRequest) null));
    server.shutdown();
  }

  @Test
  void testLoginProxy() throws RemoteException, ServerException {
    TestLoginProxy.LOGIN_COUNTER.set(0);
    TestLoginProxy.LOGOUT_COUNTER.set(0);
    TestLoginProxy.CLOSE_COUNTER.set(0);

    String clientTypeId = "clientTypeId";
    ServerConfiguration configuration = getConfiguration();
    TestServer server = new TestServer(configuration);
    ConnectionRequest connectionRequest = ConnectionRequest.builder().user(UNIT_TEST_USER).clientTypeId(clientTypeId).build();
    ServerTest connection = server.connect(connectionRequest);
    assertNotNull(connection);
    assertEquals(connectionRequest.getClientId(), connection.getRemoteClient().getClientId());

    server.disconnect(connectionRequest.getClientId());

    connection = server.connect(connectionRequest);
    assertEquals(2, TestLoginProxy.LOGIN_COUNTER.get());
    assertNotNull(connection);
    assertEquals(connectionRequest.getClientId(), connection.getRemoteClient().getClientId());

    server.disconnect(connectionRequest.getClientId());
    assertEquals(2, TestLoginProxy.LOGOUT_COUNTER.get());

    connection = server.connect(connectionRequest);
    assertEquals(3, TestLoginProxy.LOGIN_COUNTER.get());
    assertNotNull(connection);
    assertEquals(connectionRequest.getClientId(), connection.getRemoteClient().getClientId());

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
            .user(User.user(UNIT_TEST_USER.getUsername(), "test".toCharArray())).clientId(connectionId).clientTypeId(clientTypeId).build();

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
            .user(User.user("test", UNIT_TEST_USER.getPassword())).clientId(connectionId).clientTypeId(clientTypeId).build();

    server.connect(connectionRequest);

    //try to steal the connection using the same connectionId, but incorrect user credentials
    assertThrows(ServerAuthenticationException.class, () -> server.connect(connectionRequest2));

    server.shutdown();
  }

  @Test
  void admin() throws RemoteException {
    TestServer server = new TestServer();
    ServerAdmin admin = server.getAdmin();
    admin.getClients();
    admin.getClientTypes();
    admin.getConnectionCount();
    admin.setConnectionLimit(10);
    admin.getConnectionLimit();
    admin.getMaxMemory();
    admin.getAllocatedMemory();
    admin.getClients("test");
    admin.getUsers();
    admin.getClients(User.user("test"));
    admin.getSystemProperties();
    admin.getThreadStatistics();
    admin.getGcEvents(System.currentTimeMillis());
    admin.getRequestsPerSecond();
    admin.getSystemCpuLoad();
    admin.getProcessCpuLoad();
    ServerInformation serverInformation = admin.getServerInformation();
    serverInformation.getServerName();
    serverInformation.getServerId();
    serverInformation.getServerPort();
    serverInformation.getServerVersion();
    serverInformation.getLocale();
    serverInformation.getTimeZone();
    serverInformation.getStartTime();
    ServerStatistics serverStatistics = admin.getServerStatistics(System.currentTimeMillis());
    serverStatistics.getConnectionCount();
    serverStatistics.getConnectionLimit();
    serverStatistics.getUsedMemory();
    ThreadStatistics threadStatistics = serverStatistics.getThreadStatistics();
    threadStatistics.getThreadCount();
    threadStatistics.getDaemonThreadCount();
    threadStatistics.getThreadStateCount();
    serverStatistics.getAllocatedMemory();
    serverStatistics.getGcEvents();
    serverStatistics.getConnectionCount();
    serverStatistics.getMaximumMemory();
    serverStatistics.getConnectionLimit();
    serverStatistics.getProcessCpuLoad();
    serverStatistics.getSystemCpuLoad();
    serverStatistics.getRequestsPerSecond();
    serverStatistics.getTimestamp();
    admin.shutdown();
  }

  private static class ServerTestImpl implements ServerTest {

    private final RemoteClient remoteClient;

    public ServerTestImpl(RemoteClient remoteClient) {
      this.remoteClient = remoteClient;
    }

    @Override
    public RemoteClient getRemoteClient() throws RemoteException {
      return remoteClient;
    }
  }

  private interface ServerTest extends Remote {
    RemoteClient getRemoteClient() throws RemoteException;
  }

  private static ServerConfiguration getConfiguration() {
    return ServerConfiguration.builder(PORT).serverName("remoteServerTestServer").build();
  }

  private static final class TestServer extends AbstractServer<ServerTest, ServerAdmin> {

    private static final ServerConfiguration CONFIGURATION = getConfiguration();

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
    public ServerAdmin getServerAdmin(User user) throws RemoteException, ServerAuthenticationException {
      return getAdmin();
    }

    @Override
    protected void disconnect(ServerTest connection) {}

    @Override
    protected void maintainConnections(Collection<ClientConnection<ServerTest>> connections) throws RemoteException {}

    @Override
    public int getServerLoad() {
      return 0;
    }
  }

  public static final class TestLoginProxy implements LoginProxy {

    static final AtomicInteger LOGIN_COUNTER = new AtomicInteger();
    static final AtomicInteger LOGOUT_COUNTER = new AtomicInteger();
    static final AtomicInteger CLOSE_COUNTER = new AtomicInteger();

    @Override
    public String getClientTypeId() {
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
