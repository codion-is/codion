/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.credentials.server;

import is.codion.common.CredentialsProvider;
import is.codion.common.user.User;

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
    final User scott = User.parseUser("scott:tiger");
    final int registryPort = 2099;

    final CredentialsServer server = new CredentialsServer(54321, registryPort, 900, 50);

    UUID token = UUID.randomUUID();
    server.addAuthenticationToken(token, scott);
    User userCredentials = provider.getCredentials(token, registryPort);
    assertEquals(scott, userCredentials);
    assertNull(provider.getCredentials(token, registryPort));

    token = UUID.randomUUID();
    server.addAuthenticationToken(token, scott);
    userCredentials = provider.getCredentials(provider.getAuthenticationToken(
            new String[] {"bla", CredentialsProvider.AUTHENTICATION_TOKEN_PREFIX + ":" + token.toString(), "bla"}), registryPort);
    assertEquals(scott, userCredentials);
    assertNull(provider.getCredentials(token, registryPort));

    assertNull(provider.getCredentials(provider.getAuthenticationToken(new String[] {"bla", "bla"})));

    server.addAuthenticationToken(token, scott);
    Thread.sleep(1300);
    //token expired and cleaned up
    assertNull(provider.getCredentials(token, registryPort));
    server.exit();
    System.clearProperty("java.rmi.server.hostname");
  }
}
