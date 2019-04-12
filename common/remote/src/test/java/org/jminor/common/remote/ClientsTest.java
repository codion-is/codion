/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.remote;

import org.jminor.common.User;
import org.jminor.common.Version;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public final class ClientsTest {

  @Test
  public void clientUtil() {
    final User user = new User("scott", "tiger".toCharArray());
    final UUID uuid = UUID.randomUUID();
    final ConnectionRequest request = Clients.connectionRequest(user, uuid, "test");
    assertEquals(user, request.getUser());
    assertEquals(uuid, request.getClientId());
    assertNull(request.getClientVersion());
    assertEquals(Version.getVersion(), request.getFrameworkVersion());
    assertEquals(uuid.hashCode(), request.hashCode());
    assertEquals("test", request.getClientTypeId());
    assertTrue(request.toString().contains(user.getUsername()));
  }
}
