/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.junit.Test;

import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class ClientLogTest {

  @Test
  public void clientLog() {
    final UUID uuid = UUID.randomUUID();
    final long currentTime = System.currentTimeMillis();
    final ClientLog log = new ClientLog(uuid, currentTime, Collections.emptyList());
    assertEquals(uuid, log.getClientId());
    assertEquals(currentTime, log.getConnectionCreationDate());
    assertEquals(log, new ClientLog(uuid, currentTime, Collections.emptyList()));
    assertEquals(uuid.hashCode(), log.hashCode());
    assertTrue(log.getEntries().isEmpty());
  }
}
