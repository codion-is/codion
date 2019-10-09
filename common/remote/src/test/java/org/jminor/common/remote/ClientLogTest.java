/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.remote;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ClientLogTest {

  @Test
  public void clientLog() {
    final UUID uuid = UUID.randomUUID();
    final LocalDateTime currentTime = LocalDateTime.now();
    final ClientLog log = new ClientLog(uuid, currentTime, emptyList());
    assertEquals(uuid, log.getClientId());
    assertEquals(currentTime, log.getConnectionCreationDate());
    assertEquals(log, new ClientLog(uuid, currentTime, emptyList()));
    assertEquals(uuid.hashCode(), log.hashCode());
    assertTrue(log.getEntries().isEmpty());
  }
}
