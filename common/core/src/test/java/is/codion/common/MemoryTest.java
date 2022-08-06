/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

import org.junit.jupiter.api.Test;

public final class MemoryTest {

  @Test
  void test() {
    Memory.allocatedMemory();
    Memory.freeMemory();
    Memory.maxMemory();
    Memory.usedMemory();
    Memory.memoryUsage();
  }
}
