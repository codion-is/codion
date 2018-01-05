/*
 * Chinook.Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.jminor.common.User;

import org.junit.Test;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class CredentialServerTest {

  @Test
  public void test() throws AlreadyBoundException, RemoteException, InterruptedException {
    System.setProperty("java.rmi.server.hostname", CredentialServer.LOCALHOST);
    final User scott = new User("scott", "tiger");
    final CredentialServer server = new CredentialServer(54321, 100, 20);

    final UUID token = UUID.randomUUID();
    server.addAuthenticationToken(token, scott);
    assertEquals(scott, Clients.getUserCredentials(token));
    assertNull(Clients.getUserCredentials(token));

    server.addAuthenticationToken(token, scott);
    Thread.sleep(200);
    //token expired and cleaned up
    assertNull(Clients.getUserCredentials(token));
    server.exit();
    System.clearProperty("java.rmi.server.hostname");
  }
}
