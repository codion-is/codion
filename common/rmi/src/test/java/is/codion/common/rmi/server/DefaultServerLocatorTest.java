/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.server;

import is.codion.common.rmi.server.exception.ServerAuthenticationException;
import is.codion.common.user.User;

import org.junit.jupiter.api.Test;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultServerLocatorTest {

  private static final String SERVER_NAME = "DefaultServerLocatorTestServer";

  private final ServerConfiguration configuration = ServerConfiguration.builder(12345)
          .serverName(SERVER_NAME)
          .sslEnabled(false)
          .build();
  private final AbstractServer<Remote, ServerAdmin> server;

  public DefaultServerLocatorTest() throws RemoteException {
    this.server = new TestServer();
  }

  @Test
  void findServer() throws RemoteException, NotBoundException {
    //flaky test, inline setup and teardown
    Server.Locator serverLocator = Server.Locator.locator();

    Registry registry = serverLocator.initializeRegistry(Registry.REGISTRY_PORT);
    registry.rebind(SERVER_NAME, server);

    assertThrows(NotBoundException.class, () -> serverLocator.findServer("localhost", SERVER_NAME, Registry.REGISTRY_PORT, 42));
    try {
      Server<Remote, ServerAdmin> server = serverLocator.findServer("localhost", SERVER_NAME, Registry.REGISTRY_PORT, -1);
      assertNotNull(server);
    }
    catch (NotBoundException e) {
      fail("Remote server not bound");
    }

    //flaky test, inline setup and teardown
    server.shutdown();
    registry.unbind(SERVER_NAME);
  }

  private class TestServer extends AbstractServer<Remote, ServerAdmin> {

    private TestServer() throws RemoteException {
      super(configuration);
    }

    @Override
    protected Remote connect(RemoteClient remoteClient) {return null;}
    @Override
    public ServerAdmin serverAdmin(User user) throws RemoteException, ServerAuthenticationException {return null;}
    @Override
    protected void disconnect(Remote connection) {}
    @Override
    protected void maintainConnections(Collection<ClientConnection<Remote>> connections) throws RemoteException {}
    @Override
    public int serverLoad() {return 0;}
  }
}
