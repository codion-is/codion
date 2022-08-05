/*
 * Copyright (c) 2016 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.client;

import is.codion.common.user.User;
import is.codion.common.version.Version;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public final class ClientsTest {

  @Test
  void connectionRequest() {
    User user = User.parse("scott:tiger");
    UUID uuid = UUID.randomUUID();
    ConnectionRequest request = ConnectionRequest.builder()
            .user(user)
            .clientId(uuid)
            .clientTypeId("test")
            .build();
    assertEquals(user, request.user());
    assertEquals(uuid, request.clientId());
    assertNull(request.clientVersion());
    assertEquals(Version.getVersion(), request.frameworkVersion());
    assertEquals(uuid.hashCode(), request.hashCode());
    assertEquals("test", request.clientTypeId());
    assertTrue(request.toString().contains(user.getUsername()));
  }
}
