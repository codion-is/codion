/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
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
    final ConnectionRequest info = Clients.connectionRequest(user, uuid, "test");
    assertEquals(user, info.getUser());
    assertEquals(uuid, info.getClientId());
    assertNull(info.getClientVersion());
    assertEquals(Version.getVersion(), info.getFrameworkVersion());
    assertEquals(uuid.hashCode(), info.hashCode());
    assertEquals("test", info.getClientTypeId());
    assertTrue(info.toString().contains(user.getUsername()));
  }
}
