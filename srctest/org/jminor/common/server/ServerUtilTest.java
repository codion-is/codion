/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurösson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class ServerUtilTest {

  private static final String SERVER_NAME = "ServerUtilTestServer";

  private AbstractRemoteServer server;

  @Before
  public void setUp() throws RemoteException {
    ServerUtil.initializeRegistry(Registry.REGISTRY_PORT);
    server = new AbstractRemoteServer(12345, SERVER_NAME) {
      @Override
      protected Remote doConnect(final ClientInfo clientInfo) {return null;}
      @Override
      protected void doDisconnect(final Remote connection) {}
      @Override
      public int getServerLoad() {return 0;}
    };
    ServerUtil.getRegistry(Registry.REGISTRY_PORT).rebind(SERVER_NAME, server);
  }

  @After
  public void tearDown() throws RemoteException, NotBoundException {
    server.shutdown();
    ServerUtil.getRegistry(Registry.REGISTRY_PORT).unbind(SERVER_NAME);
  }

  @Test
  public void getServer() throws RemoteException {
    try {
      final RemoteServer remoteServer = ServerUtil.getServer("localhost", SERVER_NAME, Registry.REGISTRY_PORT, -1);
      assertNotNull(remoteServer);
    }
    catch (NotBoundException e) {
      fail("Remote server not bound");
    }
  }
}
