/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

import org.junit.jupiter.api.Test;

public final class MemoryTest {

  @Test
  void test() {
    Memory.getAllocatedMemory();
    Memory.getFreeMemory();
    Memory.getMaxMemory();
    Memory.getUsedMemory();
    Memory.getMemoryUsage();
  }
}
