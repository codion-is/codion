/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.credentials.server;

import org.jminor.common.CredentialsProvider;
import org.jminor.common.user.User;
import org.jminor.common.user.Users;

import org.junit.jupiter.api.Test;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public final class CredentialsServerTest {

  @Test
  public void test() throws AlreadyBoundException, RemoteException, InterruptedException {
    final CredentialsProvider provider = CredentialsProvider.credentialsProvider();

    System.setProperty("java.rmi.server.hostname", CredentialsServer.LOCALHOST);
    final User scott = Users.parseUser("scott:tiger");
    final CredentialsServer server = new CredentialsServer(54321, 900, 50);

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
    Thread.sleep(1300);
    //token expired and cleaned up
    assertNull(provider.getCredentials(token));
    server.exit();
    System.clearProperty("java.rmi.server.hostname");
  }
}
