/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.credentials.server;

import is.codion.common.credentials.CredentialsException;
import is.codion.common.credentials.CredentialsProvider;
import is.codion.common.user.User;

import org.junit.jupiter.api.Test;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public final class CredentialsServerTest {

  @Test
  void test() throws AlreadyBoundException, RemoteException, InterruptedException, CredentialsException {
    try {
      CredentialsProvider provider = CredentialsProvider.credentialsProvider();

      CredentialsService.REGISTRY_PORT.set(12345);
      System.setProperty("java.rmi.server.hostname", CredentialsServer.LOCALHOST);
      User scott = User.parse("scott:tiger");
      int serverPort = 1100;

      CredentialsServer server = new CredentialsServer(serverPort, 900, 50);

      UUID token = UUID.randomUUID();
      server.addAuthenticationToken(token, scott);
      User userCredentials = provider.getCredentials(token);
      assertEquals(scott, userCredentials);
      assertNull(provider.getCredentials(token));

      token = UUID.randomUUID();
      server.addAuthenticationToken(token, scott);
      userCredentials = provider.getCredentials(provider.getAuthenticationToken(
              new String[] {"bla", CredentialsProvider.AUTHENTICATION_TOKEN_PREFIX + ":" + token, "bla"}));
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
    catch (AlreadyBoundException | RemoteException | CredentialsException | InterruptedException e) {
      e.printStackTrace();
      throw e;
    }
  }
}
