/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.tools;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

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
    assertTrue(logger.getLastAccessTime() > 0);
    assertNotNull(logger.getLastAccessMessage());
    assertTrue(logger.getLastExitTime() > 0);

    assertEquals(1, logger.size());
    logger.setEnabled(false);
    assertEquals(1, logger.size());
  }

  @Test
  public void singleLevelLogging() {
    final MethodLogger logger = new MethodLogger(10);
    logger.logAccess("method");
    logger.logExit("method");

    assertEquals(1, logger.size());
    final MethodLogger.Entry entry = logger.getEntryAt(0);
    assertEquals("method", entry.getMethod());

    logger.logAccess("method2");
    logger.logExit("method2");

    assertEquals(2, logger.size());
    final MethodLogger.Entry entry2 = logger.getEntryAt(1);
    assertEquals("method2", entry2.getMethod());
  }

  @Test
  public void twoLevelLogging() {
    final MethodLogger logger = new MethodLogger(10);
    logger.logAccess("method");
    logger.logAccess("subMethod");
    logger.logExit("subMethod");
    logger.logAccess("subMethod2");
    logger.logExit("subMethod2");
    logger.logExit("method");

    assertEquals(1, logger.size());

    final MethodLogger.Entry lastEntry = logger.getEntryAt(0);
    assertEquals("method", lastEntry.getMethod());
    assertTrue(lastEntry.containsSubLog());
    final List<MethodLogger.Entry> subLog = lastEntry.getSubLog();
    assertEquals(2, subLog.size());
    final MethodLogger.Entry subEntry = subLog.get(0);
    assertEquals("subMethod", subEntry.getMethod());
    assertFalse(subEntry.containsSubLog());
  }

  @Test
  public void twoLevelLoggingSameMethodName() {
    final MethodLogger logger = new MethodLogger(10);
    logger.logAccess("method");
    logger.logAccess("method");
    logger.logExit("method");
    logger.logAccess("method");
    logger.logExit("method");
    logger.logExit("method");

    assertEquals(1, logger.size());

    final MethodLogger.Entry lastEntry = logger.getEntryAt(0);
    assertEquals("method", lastEntry.getMethod());
    assertTrue(lastEntry.containsSubLog());
    final List<MethodLogger.Entry> subLog = lastEntry.getSubLog();
    assertEquals(2, subLog.size());
    final MethodLogger.Entry subEntry = subLog.get(0);
    assertEquals("method", subEntry.getMethod());
    assertFalse(subEntry.containsSubLog());
  }

  @Test
  public void threeLevelLogging() {
    final MethodLogger logger = new MethodLogger(10);
    logger.logAccess("one");
    logger.logAccess("two");
    logger.logAccess("three");
    logger.logExit("three");
    logger.logExit("two");
    logger.logAccess("two2");
    logger.logAccess("three2");
    logger.logExit("three2");
    logger.logExit("two2");
    logger.logExit("one");

    assertEquals(1, logger.size());

    final MethodLogger.Entry entry = logger.getEntryAt(0);
    assertEquals("one", entry.getMethod());
    assertTrue(entry.containsSubLog());
    final List<MethodLogger.Entry> subLog = entry.getSubLog();
    assertEquals(2, subLog.size());
    final MethodLogger.Entry subEntry1 = subLog.get(0);
    assertEquals("two", subEntry1.getMethod());
    assertTrue(entry.containsSubLog());
    final MethodLogger.Entry subEntry2 = subLog.get(1);
    assertEquals("two2", subEntry2.getMethod());
    assertTrue(entry.containsSubLog());

    final List<MethodLogger.Entry> subSubLog = subEntry1.getSubLog();
    final MethodLogger.Entry subSubEntry = subSubLog.get(0);
    assertEquals("three", subSubEntry.getMethod());
    assertFalse(subSubEntry.containsSubLog());
    final List<MethodLogger.Entry> subSubLog2 = subEntry2.getSubLog();
    final MethodLogger.Entry subSubEntry2 = subSubLog2.get(0);
    assertEquals("three2", subSubEntry2.getMethod());
    assertFalse(subSubEntry2.containsSubLog());
  }

  @Test(expected = IllegalStateException.class)
  public void exitBeforeAccess() {
    final MethodLogger logger = new MethodLogger(10);
    logger.logAccess("method");
    logger.logExit("method");
    logger.logExit("method");
  }

  @Test(expected = IllegalStateException.class)
  public void wrongMethodName() {
    final MethodLogger logger = new MethodLogger(10);
    logger.logAccess("method");
    logger.logExit("anotherMethod");
  }

  @Test
  public void testEntry() {
    final MethodLogger.Entry entry = new MethodLogger.Entry("method", "message", 1000, 1000000000);
    assertFalse(entry.isComplete());

    entry.setExitMessage("exit");
    entry.setExitTime(1200, 1200000000);
    assertTrue(entry.isComplete());

    final MethodLogger.Entry newEntry = new MethodLogger.Entry("method", "message", 1100, 1100000000);
    newEntry.setExitMessage("exit");

    assertEquals("method", entry.getMethod());
    assertEquals("message", entry.getAccessMessage());
    assertEquals(1000, entry.getAccessTime());
    assertEquals(1200, entry.getExitTime());
    assertEquals(200, entry.getDelta());
    assertEquals(200000000, entry.getDeltaNano());
  }
}
