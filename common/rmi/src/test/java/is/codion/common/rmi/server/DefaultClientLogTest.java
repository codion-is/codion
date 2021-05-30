/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurösson. All Rights Reserved.
 */
package is.codion.common.rmi.server;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultClientLogTest {

  @Test
  void clientLog() {
    final UUID uuid = UUID.randomUUID();
    final LocalDateTime currentTime = LocalDateTime.now();
    final ClientLog log = ClientLog.clientLog(uuid, currentTime, emptyList());
    assertEquals(uuid, log.getClientId());
    assertEquals(currentTime, log.getConnectionCreationDate());
    assertEquals(log, ClientLog.clientLog(uuid, currentTime, emptyList()));
    assertEquals(uuid.hashCode(), log.hashCode());
    assertTrue(log.getEntries().isEmpty());
  }
}
