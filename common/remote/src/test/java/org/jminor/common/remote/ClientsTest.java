/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.remote;

import org.jminor.common.user.User;
import org.jminor.common.user.Users;
import org.jminor.common.version.Versions;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public final class ClientsTest {

  @Test
  public void connectionRequest() {
    final User user = Users.parseUser("scott:tiger");
    final UUID uuid = UUID.randomUUID();
    final ConnectionRequest request = Clients.connectionRequest(user, uuid, "test");
    assertEquals(user, request.getUser());
    assertEquals(uuid, request.getClientId());
    assertNull(request.getClientVersion());
    assertEquals(Versions.getVersion(), request.getFrameworkVersion());
    assertEquals(uuid.hashCode(), request.hashCode());
    assertEquals("test", request.getClientTypeId());
    assertTrue(request.toString().contains(user.getUsername()));
  }
}
