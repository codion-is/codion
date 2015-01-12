/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.jminor.common.model.User;
import org.junit.Test;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import static org.junit.Assert.*;

public class AbstractServerTest {

  @Test
  public void testConnectionCount() throws RemoteException, ServerException.ServerFullException, ServerException.LoginException {
    final TestServer server = new TestServer(1234, "remoteServerTestServer");
    final String clientTypeID = "clientTypeID";
    final ConnectionInfo connectionInfo = ClientUtil.connectionInfo(User.UNIT_TEST_USER, UUID.randomUUID(), clientTypeID);
    final ConnectionInfo connectionInfo2 = ClientUtil.connectionInfo(User.UNIT_TEST_USER, UUID.randomUUID(), clientTypeID);
    final ConnectionInfo connectionInfo3 = ClientUtil.connectionInfo(User.UNIT_TEST_USER, UUID.randomUUID(), clientTypeID);
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
  public void testConnectionLimitReached() throws RemoteException, ServerException.ServerFullException, ServerException.LoginException {
    final TestServer server = new TestServer(1234, "remoteServerTestServer");
    final String clientTypeID = "clientTypeID";
    final ConnectionInfo clientInfo = ClientUtil.connectionInfo(User.UNIT_TEST_USER, UUID.randomUUID(), clientTypeID);
    final ConnectionInfo clientInfo2 = ClientUtil.connectionInfo(User.UNIT_TEST_USER, UUID.randomUUID(), clientTypeID);
    server.setConnectionLimit(1);
    server.connect(clientInfo);
    server.connect(clientInfo2);
  }

  @Test
  public void testConnect() throws RemoteException, ServerException.ServerFullException, ServerException.LoginException {
    final TestServer server = new TestServer(1234, "remoteServerTestServer");
    final String clientTypeID = "clientTypeID";
    final ConnectionInfo connectionInfo = ClientUtil.connectionInfo(User.UNIT_TEST_USER, UUID.randomUUID(), clientTypeID);
    final ServerTest connection = server.connect(connectionInfo);
    assertNotNull(connection);
    final ServerTest connection2 = server.connect(connectionInfo);
    assertTrue(connection == connection2);
    server.disconnect(connectionInfo.getClientID());
    final ServerTest connection3 = server.connect(connectionInfo);
    assertFalse(connection == connection3);
    assertNotNull(server.getServerInfo());
    try {
      server.connect(null);
      fail("Should not be able to connect with null parameters");
    }
    catch (final IllegalArgumentException ignored) {}
  }

  @Test
  public void testLoginProxy() throws RemoteException, ServerException.ServerFullException, ServerException.LoginException {
    final TestServer server = new TestServer(1234, "remoteServerTestServer");
    final String clientTypeID = "clientTypeID";
    final ConnectionInfo connectionInfo = ClientUtil.connectionInfo(User.UNIT_TEST_USER, UUID.randomUUID(), clientTypeID);
    final ClientInfo proxyClientInfo = ServerUtil.clientInfo(ClientUtil.connectionInfo(User.UNIT_TEST_USER, UUID.randomUUID(), clientTypeID));
    ServerTest connection = server.connect(connectionInfo);
    assertNotNull(connection);
    assertEquals(connectionInfo.getClientID(), connection.getClientInfo().getClientID());
    final Collection<Object> closeIndicator = new ArrayList<>();
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
        closeIndicator.add(new Object());
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
    assertTrue(!closeIndicator.isEmpty());
  }

  @Test(expected = IllegalArgumentException.class)
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
    public ClientInfo getClientInfo() throws RemoteException;
  }

  private static final class TestServer extends AbstractServer<ServerTest> {

    private TestServer(final int serverPort, final String serverName) throws RemoteException {
      super(serverPort, serverName);
    }

    @Override
    protected ServerTest doConnect(final ClientInfo clientInfo) {
      return new ServerTestImpl(clientInfo);
    }

    @Override
    protected void doDisconnect(final ServerTest connection) {}

    @Override
    public int getServerLoad() {
      return 0;
    }
  }
}
