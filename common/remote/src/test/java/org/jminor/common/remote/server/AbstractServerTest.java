/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.remote.server;

import org.jminor.common.remote.client.Clients;
import org.jminor.common.remote.client.ConnectionRequest;
import org.jminor.common.remote.server.exception.ConnectionNotAvailableException;
import org.jminor.common.remote.server.exception.ConnectionValidationException;
import org.jminor.common.remote.server.exception.ServerAuthenticationException;
import org.jminor.common.remote.server.exception.ServerException;
import org.jminor.common.user.User;
import org.jminor.common.user.Users;

import org.junit.jupiter.api.Test;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class AbstractServerTest {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("jminor.test.user", "scott:tiger"));
  public static final int PORT = 1234;

  @Test
  public void testConnectionCount() throws RemoteException, ServerException {
    final TestServer server = new TestServer();
    final String clientTypeId = "clientTypeId";
    final ConnectionRequest connectionRequest = Clients.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(), clientTypeId);
    final ConnectionRequest connectionRequest2 = Clients.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(), clientTypeId);
    final ConnectionRequest connectionRequest3 = Clients.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(), clientTypeId);
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
  }

  @Test
  public void testConnectionLimitReached() throws RemoteException, ServerException {
    final TestServer server = new TestServer();
    final String clientTypeId = "clientTypeId";
    final ConnectionRequest connectionRequest = Clients.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(), clientTypeId);
    final ConnectionRequest connectionRequest2 = Clients.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(), clientTypeId);
    server.setConnectionLimit(1);
    assertEquals(1, server.getConnectionLimit());
    server.connect(connectionRequest);
    assertThrows(ConnectionNotAvailableException.class, () -> server.connect(connectionRequest2));
  }

  @Test
  public void testConnect() throws RemoteException, ServerException {
    final TestServer server = new TestServer();
    final String clientTypeId = "clientTypeId";
    final ConnectionRequest connectionRequest = Clients.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(), clientTypeId);
    final ServerTest connection = server.connect(connectionRequest);
    assertNotNull(connection);
    final ServerTest connection2 = server.connect(connectionRequest);
    assertSame(connection, connection2);
    final Map<RemoteClient, ServerTest> connections = server.getConnections();
    assertEquals(1, connections.size());
    assertEquals(connection, connections.get(connectionRequest));
    assertEquals(connection, server.getConnection(connectionRequest.getClientId()));
    assertNotNull(server.getConnection(connectionRequest.getClientId()));
    server.disconnect(connectionRequest.getClientId());
    server.disconnect(null);
    assertNull(server.getConnection(connectionRequest.getClientId()));
    final ServerTest connection3 = server.connect(connectionRequest);
    assertNotSame(connection, connection3);
    assertNotNull(server.getServerInformation());
    assertThrows(NullPointerException.class, () -> server.connect(null));
  }

  @Test
  public void testLoginProxy() throws RemoteException, ServerException {
    final String clientTypeId = "clientTypeId";
    final ServerConfiguration configuration = getConfiguration();
    configuration.setLoginProxyClassNames(Collections.singletonList(TestLoginProxy.class.getName()));
    configuration.setSharedLoginProxyClassNames(Collections.singletonList(TestLoginProxy.class.getName()));
    final TestServer server = new TestServer(configuration);
    final ConnectionRequest connectionRequest = Clients.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(), clientTypeId);
    ServerTest connection = server.connect(connectionRequest);
    assertNotNull(connection);
    assertEquals(connectionRequest.getClientId(), connection.getRemoteClient().getClientId());

    server.disconnect(connectionRequest.getClientId());

    connection = server.connect(connectionRequest);
    assertEquals(4, TestLoginProxy.LOGIN_COUNTER.get());
    assertNotNull(connection);
    assertEquals(connectionRequest.getClientId(), connection.getRemoteClient().getClientId());

    server.disconnect(connectionRequest.getClientId());
    assertEquals(4, TestLoginProxy.LOGOUT_COUNTER.get());

    connection = server.connect(connectionRequest);
    assertEquals(6, TestLoginProxy.LOGIN_COUNTER.get());
    assertNotNull(connection);
    assertEquals(connectionRequest.getClientId(), connection.getRemoteClient().getClientId());

    server.shutdown();
    assertEquals(6, TestLoginProxy.LOGOUT_COUNTER.get());
    assertEquals(2, TestLoginProxy.CLOSE_COUNTER.get());
  }

  @Test
  public void testConnectionValidator() throws RemoteException, ServerException {
    final String clientTypeId = "clientTypeId";
    final ServerConfiguration configuration = getConfiguration();
    configuration.setConnectionValidatorClassNames(Collections.singletonList(TestConnectionValidator.class.getName()));
    final TestServer server = new TestServer(configuration);
    final ConnectionRequest connectionRequest = Clients.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(), clientTypeId);
    final ServerTest connection = server.connect(connectionRequest);
    assertNotNull(connection);
    assertEquals(connectionRequest.getClientId(), connection.getRemoteClient().getClientId());

    server.disconnect(connectionRequest.getClientId());

    server.disconnect(connectionRequest.getClientId());
    assertThrows(ConnectionValidationException.class, () -> server.connect(connectionRequest));
  }

  @Test
  public void connectionTheftWrongPassword() throws RemoteException, ServerException {
    final TestServer server = new TestServer();
    final String clientTypeId = "clientTypeId";

    final UUID connectionId = UUID.randomUUID();
    final ConnectionRequest connectionRequest = Clients.connectionRequest(UNIT_TEST_USER, connectionId, clientTypeId);
    final ConnectionRequest connectionRequest2 = Clients.connectionRequest(
            Users.user(UNIT_TEST_USER.getUsername(), "test".toCharArray()), connectionId, clientTypeId);

    server.connect(connectionRequest);

    //try to steal the connection using the same connectionId, but incorrect user credentials
    assertThrows(ServerAuthenticationException.class, () -> server.connect(connectionRequest2));
  }

  @Test
  public void connectionTheftWrongUsername() throws RemoteException, ServerException {
    final TestServer server = new TestServer();
    final String clientTypeId = "clientTypeId";

    final UUID connectionId = UUID.randomUUID();
    final ConnectionRequest connectionRequest = Clients.connectionRequest(UNIT_TEST_USER, connectionId, clientTypeId);
    final ConnectionRequest connectionRequest2 = Clients.connectionRequest(
            Users.user("test", UNIT_TEST_USER.getPassword()), connectionId, clientTypeId);

    server.connect(connectionRequest);

    //try to steal the connection using the same connectionId, but incorrect user credentials
    assertThrows(ServerAuthenticationException.class, () -> server.connect(connectionRequest2));
  }

  private static class ServerTestImpl implements ServerTest {

    private final RemoteClient remoteClient;

    public ServerTestImpl(final RemoteClient remoteClient) {
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
    return ServerConfiguration.configuration(PORT).setServerName("remoteServerTestServer");
  }

  private static final class TestServer extends AbstractServer<ServerTest, Remote> {

    private TestServer() throws RemoteException {
      this(getConfiguration());
    }

    private TestServer(final ServerConfiguration configuration) throws RemoteException {
      super(configuration);
    }

    @Override
    protected ServerTest doConnect(final RemoteClient remoteClient) {
      return new ServerTestImpl(remoteClient);
    }

    @Override
    public Remote getServerAdmin(final User user) throws RemoteException, ServerAuthenticationException {
      return null;
    }

    @Override
    protected void doDisconnect(final ServerTest connection) {}

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
      return "clientTypeId";
    }
    @Override
    public RemoteClient doLogin(final RemoteClient remoteClient) {
      LOGIN_COUNTER.incrementAndGet();
      return remoteClient;
    }
    @Override
    public void doLogout(final RemoteClient remoteClient) {
      LOGOUT_COUNTER.incrementAndGet();
    }
    @Override
    public void close() {
      CLOSE_COUNTER.incrementAndGet();
    }
  }

  public static final class TestConnectionValidator implements ConnectionValidator {
    static final AtomicInteger COUNTER = new AtomicInteger();
    @Override
    public String getClientTypeId() {
      return "clientTypeId";
    }
    @Override
    public void validate(final ConnectionRequest connectionRequest) throws ConnectionValidationException {
      if (COUNTER.getAndIncrement() > 0) {
        throw new ConnectionValidationException("Testing");
      }
    }
  }
}
