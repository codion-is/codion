/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.credentials;

import is.codion.common.user.User;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public final class CredentialsProviderTest {

  @Test
  void getAuthenticationToken() {
    CredentialsProvider credentialsProvider = authenticationToken -> User.user("test");
    UUID uuid = UUID.randomUUID();

    assertNull(credentialsProvider.getAuthenticationToken(null));
    assertNull(credentialsProvider.getAuthenticationToken(new String[0]));
    assertNull(credentialsProvider.getAuthenticationToken(new String[] {"hello", "hello2"}));
    assertEquals(uuid, credentialsProvider.getAuthenticationToken(new String[] {"hello", "authenticationToken:" + uuid, "hello2"}));
  }
}
