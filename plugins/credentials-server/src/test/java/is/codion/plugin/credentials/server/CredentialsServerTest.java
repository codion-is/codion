/*
 * Copyright (c) 2017 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.credentials.server;

import is.codion.common.credentials.CredentialsException;
import is.codion.common.credentials.CredentialsProvider;
import is.codion.common.user.User;

import org.junit.jupiter.api.Test;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.util.Optional;
import java.util.UUID;

import static is.codion.plugin.credentials.server.CredentialsServer.credentialsServer;
import static org.junit.jupiter.api.Assertions.*;

public final class CredentialsServerTest {

  @Test
  void test() throws AlreadyBoundException, RemoteException, InterruptedException, CredentialsException {
    try {
      Optional<CredentialsProvider> optionalProvider = CredentialsProvider.instance();
      assertTrue(optionalProvider.isPresent());
      CredentialsProvider provider = optionalProvider.get();

      CredentialsService.REGISTRY_PORT.set(12345);
      System.setProperty("java.rmi.server.hostname", CredentialsServer.LOCALHOST);
      User scott = User.parse("scott:tiger");
      int serverPort = 1100;

      CredentialsServer server = credentialsServer(serverPort, 900, 50);

      UUID token = UUID.randomUUID();
      server.addAuthenticationToken(token, scott);
      Optional<User> optionalUser = provider.credentials(token);
      assertTrue(optionalUser.isPresent());
      User userCredentials = optionalUser.get();
      assertEquals(scott, userCredentials);
      assertFalse(provider.credentials(token).isPresent());

      token = UUID.randomUUID();
      server.addAuthenticationToken(token, scott);
      Optional<UUID> optionalUUID = CredentialsProvider.authenticationToken(
              new String[]{"bla", CredentialsProvider.AUTHENTICATION_TOKEN_PREFIX + ":" + token, "bla"});
      assertTrue(optionalUUID.isPresent());
      optionalUser = provider.credentials(optionalUUID.get());
      assertTrue(optionalUser.isPresent());
      userCredentials = optionalUser.get();
      assertEquals(scott, userCredentials);
      assertFalse(provider.credentials(token).isPresent());

      assertThrows(NullPointerException.class, () -> provider.credentials(null));

      server.addAuthenticationToken(token, scott);
      Thread.sleep(1300);
      //token expired and cleaned up
      assertFalse(provider.credentials(token).isPresent());
      server.exit();
      System.clearProperty("java.rmi.server.hostname");
    }
    catch (AlreadyBoundException | RemoteException | CredentialsException | InterruptedException e) {
      e.printStackTrace();
      throw e;
    }
  }
}
