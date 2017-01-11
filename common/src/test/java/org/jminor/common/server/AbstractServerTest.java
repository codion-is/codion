/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
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
    final String clientTypeID = "clientTypeID";
    final ConnectionInfo connectionInfo = ClientUtil.connectionInfo(UNIT_TEST_USER, UUID.randomUUID(), clientTypeID);
    final ConnectionInfo connectionInfo2 = ClientUtil.connectionInfo(UNIT_TEST_USER, UUID.randomUUID(), clientTypeID);
    final ConnectionInfo connectionInfo3 = ClientUtil.connectionInfo(UNIT_TEST_USER, UUID.randomUUID(), clientTypeID);
    server.connect(connectionInfo);
    assertEquals(1, server.getConnectionCount());
    server.connect(connectionInfo2);
    assertEquals(2, server.getConnectionCount());
    server.disconnect(connectionInfo.getClientID());
    assertEquals(1, server.getConnectionCount());
    server.connect(connectionInfo);
    assertEquals(2, server.getConnectionCount());
    server.connect(connectionInfo3);
    assertEquals(3, server.getConnectionCount());
    server.disconnect(connectionInfo3.getClientID());
    assertEquals(2, server.getConnectionCount());
    server.disconnect(connectionInfo2.getClientID());
    assertEquals(1, server.getConnectionCount());
    server.disconnect(connectionInfo.getClientID());
    assertEquals(0, server.getConnectionCount());
  }

  @Test(expected = ServerException.ServerFullException.class)
  public void testConnectionLimitReached() throws RemoteException, ServerException {
    final TestServer server = new TestServer(1234, "remoteServerTestServer");
    final String clientTypeID = "clientTypeID";
    final ConnectionInfo clientInfo = ClientUtil.connectionInfo(UNIT_TEST_USER, UUID.randomUUID(), clientTypeID);
    final ConnectionInfo clientInfo2 = ClientUtil.connectionInfo(UNIT_TEST_USER, UUID.randomUUID(), clientTypeID);
    server.setConnectionLimit(1);
    assertEquals(1, server.getConnectionLimit());
    server.connect(clientInfo);
    server.connect(clientInfo2);
  }

  @Test
  public void testConnect() throws RemoteException, ServerException {
    final TestServer server = new TestServer(1234, "remoteServerTestServer");
    final String clientTypeID = "clientTypeID";
    final ConnectionInfo connectionInfo = ClientUtil.connectionInfo(UNIT_TEST_USER, UUID.randomUUID(), clientTypeID);
    final ServerTest connection = server.connect(connectionInfo);
    assertNotNull(connection);
    final ServerTest connection2 = server.connect(connectionInfo);
    assertTrue(connection == connection2);
    final Map<ClientInfo, ServerTest> connections = server.getConnections();
    assertEquals(1, connections.size());
    assertEquals(connection, connections.get(connectionInfo));
    assertEquals(connection, server.getConnection(connectionInfo.getClientID()));
    assertTrue(server.containsConnection(connectionInfo.getClientID()));
    server.disconnect(connectionInfo.getClientID());
    server.disconnect(null);
    assertFalse(server.containsConnection(connectionInfo.getClientID()));
    final ServerTest connection3 = server.connect(connectionInfo);
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
    final String clientTypeID = "clientTypeID";
    final ConnectionInfo connectionInfo = ClientUtil.connectionInfo(UNIT_TEST_USER, UUID.randomUUID(), clientTypeID);
    final ClientInfo proxyClientInfo = ServerUtil.clientInfo(ClientUtil.connectionInfo(UNIT_TEST_USER, UUID.randomUUID(), clientTypeID));
    ServerTest connection = server.connect(connectionInfo);
    assertNotNull(connection);
    assertEquals(connectionInfo.getClientID(), connection.getClientInfo().getClientID());
    final AtomicInteger closeIndicator = new AtomicInteger();
    final LoginProxy loginProxy = new LoginProxy() {
      @Override
      public String getClientTypeID() {
        return clientTypeID;
      }
      @Override
      public ClientInfo doLogin(final ClientInfo clientInfo) {
        return proxyClientInfo;
      }
      @Override
      public void doLogout(final ClientInfo clientInfo) {}
      @Override
      public void close() {
        closeIndicator.incrementAndGet();
      }
    };
    server.setLoginProxy(clientTypeID, loginProxy);
    server.disconnect(connectionInfo.getClientID());

    connection = server.connect(connectionInfo);
    assertNotNull(connection);
    assertEquals(proxyClientInfo, connection.getClientInfo());

    server.disconnect(connectionInfo.getClientID());

    server.setLoginProxy(connectionInfo.getClientTypeID(), null);
    connection = server.connect(connectionInfo);
    assertNotNull(connection);
    assertEquals(connectionInfo.getClientID(), connection.getClientInfo().getClientID());

    server.setLoginProxy(clientTypeID, loginProxy);
    server.shutdown();
    assertTrue(closeIndicator.get() > 0);
  }

  @Test
  public void testConnectionValidator() throws RemoteException, ServerException {
    final TestServer server = new TestServer(1234, "remoteServerTestServer");
    final String clientTypeID = "clientTypeID";
    final ConnectionInfo connectionInfo = ClientUtil.connectionInfo(UNIT_TEST_USER, UUID.randomUUID(), clientTypeID);
    ServerTest connection = server.connect(connectionInfo);
    assertNotNull(connection);
    assertEquals(connectionInfo.getClientID(), connection.getClientInfo().getClientID());
    final AtomicInteger counter = new AtomicInteger();
    final ConnectionValidator connectionValidator = new ConnectionValidator() {
      @Override
      public String getClientTypeID() {
        return clientTypeID;
      }
      @Override
      public void validate(final ConnectionInfo connectionInfo) throws ServerException.ConnectionValidationException {
        if (counter.getAndIncrement() > 0) {
          throw new ServerException.ConnectionValidationException("Testing");
        }
      }
    };
    server.setConnectionValidator(clientTypeID, connectionValidator);
    server.disconnect(connectionInfo.getClientID());

    connection = server.connect(connectionInfo);
    assertNotNull(connection);

    server.disconnect(connectionInfo.getClientID());

    try {
      server.connect(connectionInfo);
      fail("Connection validator should have prevented a second connection");
    }
    catch (final ServerException.ConnectionValidationException e) {}

    server.setConnectionValidator(connectionInfo.getClientTypeID(), null);
    connection = server.connect(connectionInfo);
    assertNotNull(connection);
    assertEquals(connectionInfo.getClientID(), connection.getClientInfo().getClientID());
  }

  @Test(expected = ServerException.AuthenticationException.class)
  public void connectionTheftWrongPassword() throws RemoteException, ServerException {
    final TestServer server = new TestServer(1234, "remoteServerTestServer");
    final String clientTypeID = "clientTypeID";

    final UUID connectionID = UUID.randomUUID();
    final ConnectionInfo connectionInfo = ClientUtil.connectionInfo(UNIT_TEST_USER, connectionID, clientTypeID);
    final ConnectionInfo connectionInfo2 = ClientUtil.connectionInfo(
            new User(UNIT_TEST_USER.getUsername(), "test"), connectionID, clientTypeID);

    final ServerTest serverTest = server.connect(connectionInfo);

    //try to steal the connection using the same connectionID, but incorrect user credentials
    server.connect(connectionInfo2);
  }

  @Test(expected = ServerException.AuthenticationException.class)
  public void connectionTheftWrongUsername() throws RemoteException, ServerException {
    final TestServer server = new TestServer(1234, "remoteServerTestServer");
    final String clientTypeID = "clientTypeID";

    final UUID connectionID = UUID.randomUUID();
    final ConnectionInfo connectionInfo = ClientUtil.connectionInfo(UNIT_TEST_USER, connectionID, clientTypeID);
    final ConnectionInfo connectionInfo2 = ClientUtil.connectionInfo(
            new User("test", UNIT_TEST_USER.getPassword()), connectionID, clientTypeID);

    final ServerTest serverTest = server.connect(connectionInfo);

    //try to steal the connection using the same connectionID, but incorrect user credentials
    server.connect(connectionInfo2);
  }

  @Test(expected = IllegalStateException.class)
  public void setLoginProxyAgain() throws RemoteException {
    final TestServer server = new TestServer(1234, "remoteServerTestServer");
    try {
      final LoginProxy proxy = new LoginProxy() {
        @Override
        public String getClientTypeID() {
          return null;
        }
        @Override
        public ClientInfo doLogin(final ClientInfo clientInfo) {
          return null;
        }
        @Override
        public void doLogout(final ClientInfo clientInfo) {}
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
        public String getClientTypeID() {return null;}
        @Override
        public void validate(final ConnectionInfo connectionInfo) throws ServerException.ConnectionValidationException {}
      };
      server.setConnectionValidator("testClientType", validator);
      server.setConnectionValidator("testClientType", validator);
    }
    finally {
      server.shutdown();
    }
  }

  private static class ServerTestImpl implements ServerTest {

    private final ClientInfo clientInfo;

    public ServerTestImpl(final ClientInfo clientInfo) {
      this.clientInfo = clientInfo;
    }

    @Override
    public ClientInfo getClientInfo() throws RemoteException{
      return clientInfo;
    }
  }

  private interface ServerTest extends Remote {
    ClientInfo getClientInfo() throws RemoteException;
  }

  private static final class TestServer extends AbstractServer<ServerTest, Remote> {

    private TestServer(final int serverPort, final String serverName) throws RemoteException {
      super(serverPort, serverName);
    }

    @Override
    protected ServerTest doConnect(final ClientInfo clientInfo) {
      return new ServerTestImpl(clientInfo);
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
