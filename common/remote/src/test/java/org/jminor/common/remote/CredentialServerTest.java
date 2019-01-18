/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.remote;

import org.jminor.common.CredentialsProvider;
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
    final CredentialsProvider provider = CredentialServer.provider();

    System.setProperty("java.rmi.server.hostname", CredentialServer.LOCALHOST);
    final User scott = new User("scott", "tiger".toCharArray());
    final CredentialServer server = new CredentialServer(54321, 200, 20);

    UUID token = UUID.randomUUID();
    server.addAuthenticationToken(token, scott);
    User userCredentials = provider.getCredentials(token);
    assertEquals(scott, userCredentials);
    assertNull(provider.getCredentials(token));

    token = UUID.randomUUID();
    server.addAuthenticationToken(token, scott);
    userCredentials = provider.getCredentials(provider.getAuthenticationToken(
            new String[] {"bla", CredentialsProvider.AUTHENTICATION_TOKEN_PREFIX + ":" + token.toString(), "bla"}));
    assertEquals(scott, userCredentials);
    assertNull(provider.getCredentials(token));

    assertNull(provider.getCredentials(provider.getAuthenticationToken(new String[] {"bla", "bla"})));

    server.addAuthenticationToken(token, scott);
    Thread.sleep(300);
    //token expired and cleaned up
    assertNull(provider.getCredentials(token));
    server.exit();
    System.clearProperty("java.rmi.server.hostname");
  }
}
