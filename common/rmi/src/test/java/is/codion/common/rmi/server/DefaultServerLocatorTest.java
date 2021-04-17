/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurösson. All Rights Reserved.
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

  private final ServerConfiguration configuration = ServerConfiguration.configuration(12345);
  private final AbstractServer<Remote, ServerAdmin> server;

  public DefaultServerLocatorTest() throws RemoteException {
    configuration.setServerName(SERVER_NAME);
    configuration.setSslEnabled(false);
    this.server = new TestServer();
  }

  @Test
  public void getServer() throws RemoteException, NotBoundException {
    //flaky test, inline setup and teardown
    final DefaultServerLocator serverLocator = new DefaultServerLocator();

    final Registry registry = serverLocator.initializeRegistry(Registry.REGISTRY_PORT);
    registry.rebind(SERVER_NAME, server);

    assertThrows(NotBoundException.class, () -> serverLocator.getServer("localhost", SERVER_NAME, Registry.REGISTRY_PORT, 42));
    try {
      final Server<Remote, ServerAdmin> server = serverLocator.getServer("localhost", SERVER_NAME, Registry.REGISTRY_PORT, -1);
      assertNotNull(server);
    }
    catch (final NotBoundException e) {
      fail("Remote server not bound");
    }

    //flaky test, inline setup and teardown
    server.shutdown();
    registry.unbind(SERVER_NAME);
  }

  private class TestServer extends AbstractServer<Remote, ServerAdmin> {

    public TestServer() throws RemoteException {
      super(configuration);
    }

    @Override
    protected Remote connect(final RemoteClient remoteClient) {return null;}
    @Override
    public ServerAdmin getServerAdmin(final User user) throws RemoteException, ServerAuthenticationException {return null;}
    @Override
    protected void disconnect(final Remote connection) {}
    @Override
    protected void maintainConnections(final Collection<ClientConnection<Remote>> connections) throws RemoteException {}
    @Override
    public int getServerLoad() {return 0;}
  }
}
