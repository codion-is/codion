/*
 * Chinook.Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.jminor.common.User;

import org.junit.Test;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class AbstractServerTest {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger"));

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

  @Test(expected = ServerException.ServerFullException.class)
  public void testConnectionLimitReached() throws RemoteException, ServerException {
    final TestServer server = new TestServer(1234, "remoteServerTestServer");
    final String clientTypeId = "clientTypeId";
    final ConnectionRequest connectionRequest = Clients.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(), clientTypeId);
    final ConnectionRequest connectionRequest2 = Clients.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(), clientTypeId);
    server.setConnectionLimit(1);
    assertEquals(1, server.getConnectionLimit());
    server.connect(connectionRequest);
    server.connect(connectionRequest2);
  }

  @Test
  public void testConnect() throws RemoteException, ServerException {
    final TestServer server = new TestServer(1234, "remoteServerTestServer");
    final String clientTypeId = "clientTypeId";
    final ConnectionRequest connectionRequest = Clients.connectionRequest(UNIT_TEST_USER, UUID.randomUUID(), clientTypeId);
    final ServerTest connection = server.connect(connectionRequest);
    assertNotNull(connection);
    final ServerTest connection2 = server.connect(connectionRequest);
    assertTrue(connection == connection2);
    final Map<RemoteClient, ServerTest> connections = server.getConnections();
    assertEquals(1, connections.size());
    assertEquals(connection, connections.get(connectionRequest));
    assertEquals(connection, server.getConnection(connectionRequest.getClientId()));
    assertTrue(server.containsConnection(connectionRequest.getClientId()));
    server.disconnect(connectionRequest.getClientId());
    server.disconnect(null);
    assertFalse(server.containsConnection(connectionRequest.getClientId()));
    final ServerTest connection3 = server.connect(connectionRequest);
    assertFalse(connection == connection3);
    assertNotNull(server.getServerInfo());
    try {
      server.connect(null);
      fail("Should not be able to connect with null parameters");
    }
    catch (final NullPointerException ignored) {/*ignored*/}
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
    final AtomicInteger closeIndicator = new AtomicInteger();
    final LoginProxy loginProxy = new LoginProxy() {
      @Override
      public String getClientTypeId() {
        return clientTypeId;
      }
      @Override
      public RemoteClient doLogin(final RemoteClient remoteClient) {
        return proxyRemoteClient;
      }
      @Override
      public void doLogout(final RemoteClient remoteClient) {}
      @Override
      public void close() {
        closeIndicator.incrementAndGet();
      }
    };
    server.setLoginProxy(clientTypeId, loginProxy);
    server.disconnect(connectionRequest.getClientId());

    connection = server.connect(connectionRequest);
    assertNotNull(connection);
    assertEquals(proxyRemoteClient, connection.getRemoteClient());

    server.disconnect(connectionRequest.getClientId());

    server.setLoginProxy(connectionRequest.getClientTypeId(), null);
    connection = server.connect(connectionRequest);
    assertNotNull(connection);
    assertEquals(connectionRequest.getClientId(), connection.getRemoteClient().getClientId());

    server.setLoginProxy(clientTypeId, loginProxy);
    server.shutdown();
    assertTrue(closeIndicator.get() > 0);
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
      public void validate(final ConnectionRequest connectionRequest) throws ServerException.ConnectionValidationException {
        if (counter.getAndIncrement() > 0) {
          throw new ServerException.ConnectionValidationException("Testing");
        }
      }
    };
    server.setConnectionValidator(clientTypeId, connectionValidator);
    server.disconnect(connectionRequest.getClientId());

    connection = server.connect(connectionRequest);
    assertNotNull(connection);

    server.disconnect(connectionRequest.getClientId());

    try {
      server.connect(connectionRequest);
      fail("Connection validator should have prevented a second connection");
    }
    catch (final ServerException.ConnectionValidationException e) {}

    server.setConnectionValidator(connectionRequest.getClientTypeId(), null);
    connection = server.connect(connectionRequest);
    assertNotNull(connection);
    assertEquals(connectionRequest.getClientId(), connection.getRemoteClient().getClientId());
  }

  @Test(expected = ServerException.AuthenticationException.class)
  public void connectionTheftWrongPassword() throws RemoteException, ServerException {
    final TestServer server = new TestServer(1234, "remoteServerTestServer");
    final String clientTypeId = "clientTypeId";

    final UUID connectionId = UUID.randomUUID();
    final ConnectionRequest connectionRequest = Clients.connectionRequest(UNIT_TEST_USER, connectionId, clientTypeId);
    final ConnectionRequest connectionRequest2 = Clients.connectionRequest(
            new User(UNIT_TEST_USER.getUsername(), "test"), connectionId, clientTypeId);

    final ServerTest serverTest = server.connect(connectionRequest);

    //try to steal the connection using the same connectionId, but incorrect user credentials
    server.connect(connectionRequest2);
  }

  @Test(expected = ServerException.AuthenticationException.class)
  public void connectionTheftWrongUsername() throws RemoteException, ServerException {
    final TestServer server = new TestServer(1234, "remoteServerTestServer");
    final String clientTypeId = "clientTypeId";

    final UUID connectionId = UUID.randomUUID();
    final ConnectionRequest connectionRequest = Clients.connectionRequest(UNIT_TEST_USER, connectionId, clientTypeId);
    final ConnectionRequest connectionRequest2 = Clients.connectionRequest(
            new User("test", UNIT_TEST_USER.getPassword()), connectionId, clientTypeId);

    final ServerTest serverTest = server.connect(connectionRequest);

    //try to steal the connection using the same connectionId, but incorrect user credentials
    server.connect(connectionRequest2);
  }

  @Test(expected = IllegalStateException.class)
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
      server.setLoginProxy("testClientType", proxy);
    }
    finally {
      server.shutdown();
    }
  }

  @Test(expected = IllegalStateException.class)
  public void setClientValidatorAgain() throws RemoteException {
    final TestServer server = new TestServer(1234, "remoteServerTestServer");
    try {
      final ConnectionValidator validator = new ConnectionValidator() {
        @Override
        public String getClientTypeId() {return null;}
        @Override
        public void validate(final ConnectionRequest connectionRequest) throws ServerException.ConnectionValidationException {}
      };
      server.setConnectionValidator("testClientType", validator);
      server.setConnectionValidator("testClientType", validator);
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
    public RemoteClient getRemoteClient() throws RemoteException{
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
    public Remote getServerAdmin(final User user) throws RemoteException, ServerException.AuthenticationException {
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
