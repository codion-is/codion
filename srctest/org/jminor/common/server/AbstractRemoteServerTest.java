/*
 * Copyright (c) 2004 - 2011, Björn Darri Sigurðsson. All Rights Reserved.
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

public class AbstractRemoteServerTest {

  @Test
  public void testConnectionCount() throws RemoteException, ServerException.ServerFullException, ServerException.LoginException {
    final RemoteServerTestServer server = new RemoteServerTestServer(1234, "remoteServerTestServer");
    final String clientTypeID = "clientTypeID";
    final ClientInfo clientInfo = new ClientInfo(UUID.randomUUID(), clientTypeID, User.UNIT_TEST_USER);
    final ClientInfo clientInfo2 = new ClientInfo(UUID.randomUUID(), clientTypeID, User.UNIT_TEST_USER);
    final ClientInfo clientInfo3 = new ClientInfo(UUID.randomUUID(), clientTypeID, User.UNIT_TEST_USER);
    server.connect(clientInfo);
    assertEquals(1, server.getConnectionCount());
    server.connect(clientInfo2);
    assertEquals(2, server.getConnectionCount());
    server.disconnect(clientInfo.getClientID());
    assertEquals(1, server.getConnectionCount());
    server.connect(clientInfo);
    assertEquals(2, server.getConnectionCount());
    server.connect(clientInfo3);
    assertEquals(3, server.getConnectionCount());
    server.disconnect(clientInfo3.getClientID());
    assertEquals(2, server.getConnectionCount());
    server.disconnect(clientInfo2.getClientID());
    assertEquals(1, server.getConnectionCount());
    server.disconnect(clientInfo.getClientID());
    assertEquals(0, server.getConnectionCount());
  }

  @Test(expected = ServerException.ServerFullException.class)
  public void testConnectionLimitReached() throws RemoteException, ServerException.ServerFullException, ServerException.LoginException {
    final RemoteServerTestServer server = new RemoteServerTestServer(1234, "remoteServerTestServer");
    final String clientTypeID = "clientTypeID";
    final ClientInfo clientInfo = new ClientInfo(UUID.randomUUID(), clientTypeID, User.UNIT_TEST_USER);
    final ClientInfo clientInfo2 = new ClientInfo(UUID.randomUUID(), clientTypeID, User.UNIT_TEST_USER);
    server.setConnectionLimit(1);
    server.connect(clientInfo);
    server.connect(clientInfo2);
  }

  @Test
  public void testConnect() throws RemoteException, ServerException.ServerFullException, ServerException.LoginException {
    final RemoteServerTestServer server = new RemoteServerTestServer(1234, "remoteServerTestServer");
    final String clientTypeID = "clientTypeID";
    final ClientInfo clientInfo = new ClientInfo(UUID.randomUUID(), clientTypeID, User.UNIT_TEST_USER);
    final RemoteServerTest connection = server.connect(clientInfo);
    assertNotNull(connection);
    final RemoteServerTest connection2 = server.connect(clientInfo);
    assertTrue(connection == connection2);
    server.disconnect(clientInfo.getClientID());
    final RemoteServerTest connection3 = server.connect(clientInfo);
    assertFalse(connection == connection3);
  }

  @Test
  public void testLoginProxy() throws RemoteException, ServerException.ServerFullException, ServerException.LoginException {
    final RemoteServerTestServer server = new RemoteServerTestServer(1234, "remoteServerTestServer");
    final String clientTypeID = "clientTypeID";
    final ClientInfo baseClientInfo = new ClientInfo(UUID.randomUUID(), clientTypeID, User.UNIT_TEST_USER);
    final ClientInfo proxyClientInfo = new ClientInfo(UUID.randomUUID(), clientTypeID, User.UNIT_TEST_USER);
    RemoteServerTest connection = server.connect(baseClientInfo);
    assertNotNull(connection);
    assertEquals(baseClientInfo, connection.getClientInfo());
    final Collection<Object> closeIndicator = new ArrayList<Object>();
    final LoginProxy loginProxy = new LoginProxy() {
      @Override
      public String getClientTypeID() {
        return clientTypeID;
      }
      @Override
      public ClientInfo doLogin(final ClientInfo clientInfo) throws ServerException.LoginException {
        return proxyClientInfo;
      }
      @Override
      public void close() {
        closeIndicator.add(new Object());
      }
    };
    server.setLoginProxy(clientTypeID, loginProxy);
    server.disconnect(baseClientInfo.getClientID());

    connection = server.connect(baseClientInfo);
    assertNotNull(connection);
    assertEquals(proxyClientInfo, connection.getClientInfo());

    server.disconnect(baseClientInfo.getClientID());

    server.setLoginProxy(baseClientInfo.getClientTypeID(), null);
    connection = server.connect(baseClientInfo);
    assertNotNull(connection);
    assertEquals(baseClientInfo, connection.getClientInfo());

    server.setLoginProxy(clientTypeID, loginProxy);
    server.shutdown();
    assertTrue(!closeIndicator.isEmpty());
  }

  private static class RemoteServerTestImpl implements RemoteServerTest {

    private final ClientInfo clientInfo;

    public RemoteServerTestImpl(final ClientInfo clientInfo) {
      this.clientInfo = clientInfo;
    }

    @Override
    public ClientInfo getClientInfo() throws RemoteException{
      return clientInfo;
    }
  }

  private interface RemoteServerTest extends Remote {
    public ClientInfo getClientInfo() throws RemoteException;
  }

  private static final class RemoteServerTestServer extends AbstractRemoteServer<RemoteServerTest> {

    private RemoteServerTestServer(final int serverPort, final String serverName) throws RemoteException {
      super(serverPort, serverName);
    }

    @Override
    protected RemoteServerTest doConnect(final ClientInfo clientInfo) throws RemoteException {
      return new RemoteServerTestImpl(clientInfo);
    }

    @Override
    protected void doDisconnect(final RemoteServerTest connection) throws RemoteException {}

    @Override
    public int getServerLoad() throws RemoteException {
      return 0;
    }
  }
}
