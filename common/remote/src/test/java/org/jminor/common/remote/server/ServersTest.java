/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurösson. All Rights Reserved.
 */
package org.jminor.common.remote.server;

import org.jminor.common.remote.server.exception.ServerAuthenticationException;
import org.jminor.common.user.User;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.time.LocalDateTime;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

public class ServersTest {

  private static final String SERVER_NAME = "ServerUtilTestServer";

  private AbstractServer server;

  @BeforeEach
  public void setUp() throws RemoteException {
    Servers.initializeRegistry(Registry.REGISTRY_PORT);
    final ServerConfiguration configuration = ServerConfiguration.configuration(12345);
    configuration.setServerName(SERVER_NAME);
    configuration.setSslEnabled(false);
    server = new AbstractServer(configuration) {
      @Override
      protected Remote doConnect(final RemoteClient remoteClient) {return null;}
      @Override
      public Remote getServerAdmin(final User user) throws RemoteException, ServerAuthenticationException {return null;}
      @Override
      protected void doDisconnect(final Remote connection) {}
      @Override
      public int getServerLoad() {return 0;}
    };
    Servers.getRegistry(Registry.REGISTRY_PORT).rebind(SERVER_NAME, server);
  }

  @AfterEach
  public void tearDown() throws RemoteException, NotBoundException {
    server.shutdown();
    Servers.getRegistry(Registry.REGISTRY_PORT).unbind(SERVER_NAME);
  }

  @Test
  public void getServer() throws RemoteException {
    try {
      final Server server = Servers.getServer("localhost", SERVER_NAME, Registry.REGISTRY_PORT, -1);
      assertNotNull(server);
    }
    catch (final NotBoundException e) {
      fail("Remote server not bound");
    }
  }

  @Test
  public void getServerWrongPort() throws RemoteException, NotBoundException {
    assertThrows(NotBoundException.class, () -> Servers.getServer("localhost", SERVER_NAME, Registry.REGISTRY_PORT, 42));
  }

  @Test
  public void clientLog() {
    final UUID uuid = UUID.randomUUID();
    final LocalDateTime currentTime = LocalDateTime.now();
    final ClientLog log = ClientLog.clientLog(uuid, currentTime, emptyList());
    assertEquals(uuid, log.getClientId());
    assertEquals(currentTime, log.getConnectionCreationDate());
    assertEquals(log, ClientLog.clientLog(uuid, currentTime, emptyList()));
    assertEquals(uuid.hashCode(), log.hashCode());
    assertTrue(log.getEntries().isEmpty());
  }
}
