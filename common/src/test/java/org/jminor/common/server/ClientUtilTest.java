/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.jminor.common.User;
import org.jminor.common.Version;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public final class ClientUtilTest {

  @Test
  public void clientUtil() {
    final User user = new User("scott", "tiger");
    final UUID uuid = UUID.randomUUID();
    final ConnectionInfo info = ClientUtil.connectionInfo(user, uuid, "test");
    assertEquals(user, info.getUser());
    assertEquals(uuid, info.getClientID());
    assertNull(info.getClientVersion());
    assertEquals(Version.getVersion(), info.getFrameworkVersion());
    assertEquals(uuid.hashCode(), info.hashCode());
    assertEquals("test", info.getClientTypeID());
    assertTrue(info.toString().contains(user.getUsername()));
  }
}
