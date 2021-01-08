/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.client;

import is.codion.common.user.User;
import is.codion.common.version.Version;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public final class ClientsTest {

  @Test
  public void connectionRequest() {
    final User user = User.parseUser("scott:tiger");
    final UUID uuid = UUID.randomUUID();
    final ConnectionRequest request = ConnectionRequest.connectionRequest(user, uuid, "test");
    assertEquals(user, request.getUser());
    assertEquals(uuid, request.getClientId());
    assertNull(request.getClientVersion());
    assertEquals(Version.getVersion(), request.getFrameworkVersion());
    assertEquals(uuid.hashCode(), request.hashCode());
    assertEquals("test", request.getClientTypeId());
    assertTrue(request.toString().contains(user.getUsername()));
  }
}
