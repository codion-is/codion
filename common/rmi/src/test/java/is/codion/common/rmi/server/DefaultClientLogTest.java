/*
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.server;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultClientLogTest {

  @Test
  void clientLog() {
    UUID uuid = UUID.randomUUID();
    ClientLog log = ClientLog.clientLog(uuid, emptyList());
    assertEquals(uuid, log.clientId());
    assertEquals(uuid.hashCode(), log.hashCode());
    assertTrue(log.entries().isEmpty());
  }
}
