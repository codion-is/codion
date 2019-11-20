/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.remote;

import org.jminor.common.User;
import org.jminor.common.remote.exception.ConnectionNotAvailableException;
import org.jminor.common.remote.exception.ConnectionValidationException;
import org.jminor.common.remote.exception.ServerAuthenticationException;
import org.jminor.common.remote.exception.ServerException;

import org.junit.jupiter.api.Test;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class AbstractServerTest {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  @Test
  public void testConnectionCount() throws RemoteException, ServerException {
    final TestServer server = new TestServer(1234, "remoteServerTestServer");
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
    final TestServer server = new TestServer(1234, "remoteServerTestServer");
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
    final TestServer server = new TestServer(1234, "remoteServerTestServer");
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
    assertNotNull(server.getServerInfo());
    assertThrows(NullPointerException.class, () -> server.connect(null));
  }

  @Test
  public void testLoginProxy() throws RemoteException, ServerException {
    final TestServer server = new TestServer(1234, "remoteServerTestServer");
    final String clientTypeId = "clientTypeId";
    final ConnectionRequest connectionRequest = Clients.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(), clientTypeId);
    final RemoteClient proxyRemoteClient = Servers.remoteClient(Clients.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(), clientTypeId));
    ServerTest connection = server.connect(connectionRequest);
    assertNotNull(connection);
    assertEquals(connectionRequest.getClientId(), connection.getRemoteClient().getClientId());
    final AtomicInteger loginCounter = new AtomicInteger();
    final AtomicInteger logoutCounter = new AtomicInteger();
    final AtomicInteger closeCounter = new AtomicInteger();
    class TestLoginProxy implements LoginProxy {
      @Override
      public String getClientTypeId() {
        return clientTypeId;
      }
      @Override
      public RemoteClient doLogin(final RemoteClient remoteClient) {
        loginCounter.incrementAndGet();
        return proxyRemoteClient;
      }
      @Override
      public void doLogout(final RemoteClient remoteClient) {
        logoutCounter.incrementAndGet();
      }
      @Override
      public void close() {
        closeCounter.incrementAndGet();
      }
    }
    final LoginProxy sharedProxy = new TestLoginProxy();
    final LoginProxy loginProxy = new TestLoginProxy();

    server.addSharedLoginProxy(sharedProxy);
    server.setLoginProxy(clientTypeId, loginProxy);
    server.disconnect(connectionRequest.getClientId());

    connection = server.connect(connectionRequest);
    assertEquals(2, loginCounter.get());
    assertNotNull(connection);
    assertEquals(proxyRemoteClient, connection.getRemoteClient());

    server.disconnect(connectionRequest.getClientId());
    assertEquals(2, logoutCounter.get());

    server.setLoginProxy(connectionRequest.getClientTypeId(), null);
    connection = server.connect(connectionRequest);
    assertEquals(3, loginCounter.get());
    assertNotNull(connection);
    assertEquals(proxyRemoteClient, connection.getRemoteClient());

    server.shutdown();
    assertEquals(3, logoutCounter.get());
    assertEquals(2, closeCounter.get());
  }

  @Test
  public void testConnectionValidator() throws RemoteException, ServerException {
    final TestServer server = new TestServer(1234, "remoteServerTestServer");
    final String clientTypeId = "clientTypeId";
    final ConnectionRequest connectionRequest = Clients.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(), clientTypeId);
    ServerTest connection = server.connect(connectionRequest);
    assertNotNull(connection);
    assertEquals(connectionRequest.getClientId(), connection.getRemoteClient().getClientId());
    final AtomicInteger counter = new AtomicInteger();
    final ConnectionValidator connectionValidator = new ConnectionValidator() {
      @Override
      public String getClientTypeId() {
        return clientTypeId;
      }
      @Override
      public void validate(final ConnectionRequest connectionRequest) throws ConnectionValidationException {
        if (counter.getAndIncrement() > 0) {
          throw new ConnectionValidationException("Testing");
        }
      }
    };
    server.setConnectionValidator(clientTypeId, connectionValidator);
    server.disconnect(connectionRequest.getClientId());

    connection = server.connect(connectionRequest);
    assertNotNull(connection);

    server.disconnect(connectionRequest.getClientId());
    assertThrows(ConnectionValidationException.class, () -> server.connect(connectionRequest));

    server.setConnectionValidator(connectionRequest.getClientTypeId(), null);
    connection = server.connect(connectionRequest);
    assertNotNull(connection);
    assertEquals(connectionRequest.getClientId(), connection.getRemoteClient().getClientId());
  }

  @Test
  public void connectionTheftWrongPassword() throws RemoteException, ServerException {
    final TestServer server = new TestServer(1234, "remoteServerTestServer");
    final String clientTypeId = "clientTypeId";

    final UUID connectionId = UUID.randomUUID();
    final ConnectionRequest connectionRequest = Clients.connectionRequest(UNIT_TEST_USER, connectionId, clientTypeId);
    final ConnectionRequest connectionRequest2 = Clients.connectionRequest(
            new User(UNIT_TEST_USER.getUsername(), "test".toCharArray()), connectionId, clientTypeId);

    server.connect(connectionRequest);

    //try to steal the connection using the same connectionId, but incorrect user credentials
    assertThrows(ServerAuthenticationException.class, () -> server.connect(connectionRequest2));
  }

  @Test
  public void connectionTheftWrongUsername() throws RemoteException, ServerException {
    final TestServer server = new TestServer(1234, "remoteServerTestServer");
    final String clientTypeId = "clientTypeId";

    final UUID connectionId = UUID.randomUUID();
    final ConnectionRequest connectionRequest = Clients.connectionRequest(UNIT_TEST_USER, connectionId, clientTypeId);
    final ConnectionRequest connectionRequest2 = Clients.connectionRequest(
            new User("test", UNIT_TEST_USER.getPassword()), connectionId, clientTypeId);

    server.connect(connectionRequest);

    //try to steal the connection using the same connectionId, but incorrect user credentials
    assertThrows(ServerAuthenticationException.class, () -> server.connect(connectionRequest2));
  }

  @Test
  public void setLoginProxyAgain() throws RemoteException {
    final TestServer server = new TestServer(1234, "remoteServerTestServer");
    try {
      final LoginProxy proxy = new LoginProxy() {
        @Override
        public String getClientTypeId() {
          return null;
        }
        @Override
        public RemoteClient doLogin(final RemoteClient remoteClient) {
          return null;
        }
        @Override
        public void doLogout(final RemoteClient remoteClient) {}
        @Override
        public void close() {}
      };
      server.setLoginProxy("testClientType", proxy);
      assertThrows(IllegalStateException.class, () -> server.setLoginProxy("testClientType", proxy));
    }
    finally {
      server.shutdown();
    }
  }

  @Test
  public void setClientValidatorAgain() throws RemoteException {
    final TestServer server = new TestServer(1234, "remoteServerTestServer");
    try {
      final ConnectionValidator validator = new ConnectionValidator() {
        @Override
        public String getClientTypeId() {return null;}
        @Override
        public void validate(final ConnectionRequest connectionRequest) throws ConnectionValidationException {}
      };
      server.setConnectionValidator("testClientType", validator);
      assertThrows(IllegalStateException.class, () -> server.setConnectionValidator("testClientType", validator));
    }
    finally {
      server.shutdown();
    }
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

  private static final class TestServer extends AbstractServer<ServerTest, Remote> {

    private TestServer(final int serverPort, final String serverName) throws RemoteException {
      super(serverPort, serverName);
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
}
