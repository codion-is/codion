package org.jminor.common.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class DbStatisticsTest {
  @Test
  public void test() {
    final DbStatistics stats = new DbStatistics(1, 2, 3, 4, 5);
    assertEquals(1, stats.getQueriesPerSecond());
    assertEquals(2, stats.getSelectsPerSecond());
    assertEquals(3, stats.getInsertsPerSecond());
    assertEquals(4, stats.getDeletesPerSecond());
    assertEquals(5, stats.getUpdatesPerSecond());
    assertTrue(stats.getTimestamp() <= System.currentTimeMillis());
  }
}
