/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.common.rmi.client;

import dev.codion.common.user.User;
import dev.codion.common.user.Users;
import dev.codion.common.version.Versions;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public final class ClientsTest {

  @Test
  public void connectionRequest() {
    final User user = Users.parseUser("scott:tiger");
    final UUID uuid = UUID.randomUUID();
    final ConnectionRequest request = ConnectionRequest.connectionRequest(user, uuid, "test");
    assertEquals(user, request.getUser());
    assertEquals(uuid, request.getClientId());
    assertNull(request.getClientVersion());
    assertEquals(Versions.getVersion(), request.getFrameworkVersion());
    assertEquals(uuid.hashCode(), request.hashCode());
    assertEquals("test", request.getClientTypeId());
    assertTrue(request.toString().contains(user.getUsername()));
  }
}
