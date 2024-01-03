/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.rmi.server;

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
    Server.Locator serverLocator = Server.Locator.builder()
            .hostName("localhost")
            .namePrefix(SERVER_NAME)
            .registryPort(Registry.REGISTRY_PORT)
            .port(42)
            .build();

    Registry registry = Server.Locator.registry(Registry.REGISTRY_PORT);
    registry.rebind(SERVER_NAME, server);

    assertThrows(NotBoundException.class, serverLocator::locateServer);
    try {
      serverLocator = Server.Locator.builder()
              .hostName("localhost")
              .namePrefix(SERVER_NAME)
              .registryPort(Registry.REGISTRY_PORT)
              .port(-1)
              .build();
      Server<Remote, ServerAdmin> server = serverLocator.locateServer();
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
    public ServerAdmin serverAdmin(User user) throws RemoteException {return null;}
    @Override
    protected void disconnect(Remote connection) {}
    @Override
    protected void maintainConnections(Collection<ClientConnection<Remote>> connections) throws RemoteException {}
    @Override
    public int serverLoad() {return 0;}
  }
}
