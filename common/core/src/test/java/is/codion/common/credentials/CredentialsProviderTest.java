/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.credentials;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public final class CredentialsProviderTest {

  @Test
  void authenticationToken() {
    UUID uuid = UUID.randomUUID();

    assertFalse(CredentialsProvider.authenticationToken(null).isPresent());
    assertFalse(CredentialsProvider.authenticationToken(new String[0]).isPresent());
    assertFalse(CredentialsProvider.authenticationToken(new String[] {"hello", "hello2"}).isPresent());
    Optional<UUID> optionalUUID = CredentialsProvider.authenticationToken(new String[]{"hello", "authenticationToken:" + uuid, "hello2"});
    assertTrue(optionalUUID.isPresent());
    assertEquals(uuid, optionalUUID.get());
  }
}
