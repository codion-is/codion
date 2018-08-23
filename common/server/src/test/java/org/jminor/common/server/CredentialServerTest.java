/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.jminor.common.User;

import org.junit.jupiter.api.Test;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public final class CredentialServerTest {

  @Test
  public void test() throws AlreadyBoundException, RemoteException, InterruptedException {
    System.setProperty("java.rmi.server.hostname", CredentialServer.LOCALHOST);
    final User scott = new User("scott", "tiger".toCharArray());
    final CredentialServer server = new CredentialServer(54321, 200, 20);

    UUID token = UUID.randomUUID();
    server.addAuthenticationToken(token, scott);
    User userCredentials = Clients.getUserCredentials(token);
    assertEquals(scott, userCredentials);
    assertNull(Clients.getUserCredentials(token));

    token = UUID.randomUUID();
    server.addAuthenticationToken(token, scott);
    userCredentials = Clients.getUserCredentials(new String[] {"bla", Clients.AUTHENTICATION_TOKEN_PREFIX + ":" + token.toString(), "bla"});
    assertEquals(scott, userCredentials);
    assertNull(Clients.getUserCredentials(token));

    assertNull(Clients.getUserCredentials(new String[] {"bla", "bla"}));

    server.addAuthenticationToken(token, scott);
    Thread.sleep(300);
    //token expired and cleaned up
    assertNull(Clients.getUserCredentials(token));
    server.exit();
    System.clearProperty("java.rmi.server.hostname");
  }
}
