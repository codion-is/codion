package org.jminor.common.model;

import static org.junit.Assert.*;
import org.junit.Test;

public class MethodLoggerTest {

  @Test
  public void test() throws Exception {
    final MethodLogger logger = new MethodLogger(10);
    assertFalse(logger.isEnabled());
    logger.setEnabled(true);

    final String methodName = "test";
    logger.logAccess(methodName, new Object[0]);
    Thread.sleep(10);
    logger.logExit(methodName, null, null);
    assertEquals(methodName, logger.getLastAccessedMethod());
    assertEquals(methodName, logger.getLastExitedMethod());
    assertNotNull(logger.getLastAccessDate());
    assertNotNull(logger.getLastAccessMessage());
    assertNotNull(logger.getLastExitDate());

    assertEquals(1, logger.getLogEntries().size());
    logger.setEnabled(false);
    assertEquals(1, logger.getLogEntries().size());
  }
}
