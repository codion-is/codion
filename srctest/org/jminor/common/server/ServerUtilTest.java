/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurösson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.jminor.common.model.Util;

import org.junit.After;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public class ServerUtilTest {

  private static final String SERVER_NAME = "ServerUtilTestServer";

  private AbstractRemoteServer server;

  @Before
  public void setUp() throws RemoteException {
    Util.initializeRegistry();
    server = new AbstractRemoteServer(12345, SERVER_NAME) {
      @Override
      protected Remote doConnect(final ClientInfo clientInfo) throws RemoteException {
        return null;
      }
      @Override
      protected void doDisconnect(final Remote connection) throws RemoteException {}
      public int getServerLoad() throws RemoteException {
        return 0;
      }
    };
    Util.getRegistry().rebind(SERVER_NAME, server);
  }

  @After
  public void tearDown() throws RemoteException, NotBoundException {
    server.shutdown();
    Util.getRegistry().unbind(SERVER_NAME);
  }

  @Test
  public void test() throws RemoteException {
    try {
      final RemoteServer remoteServer = ServerUtil.getServer("localhost", SERVER_NAME);
      assertNotNull(remoteServer);
    }
    catch (NotBoundException e) {
      fail("Remote server not bound");
    }
  }
}
