/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MethodLoggerTest {

  @Test
  public void test() throws Exception {
    final MethodLogger logger = new MethodLogger(10, false);
    assertFalse(logger.isEnabled());
    logger.setEnabled(true);

    final String methodName = "test";
    logger.logAccess(methodName);
    logger.logExit(methodName);

    assertEquals(1, logger.getEntries().size());
    logger.setEnabled(false);
    assertEquals(0, logger.getEntries().size());
  }

  @Test
  public void serialize() throws IOException, ClassNotFoundException {
    final MethodLogger logger = new MethodLogger(10, false);
    logger.setEnabled(true);
    logger.logAccess("method");
    logger.logAccess("method2");
    logger.logExit("method2");
    logger.logExit("method");

    final ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
    new ObjectOutputStream(byteOut).writeObject(logger.getEntries());
    final List<MethodLogger.Entry> readLogger = (List<MethodLogger.Entry>) new ObjectInputStream(
            new ByteArrayInputStream(byteOut.toByteArray())).readObject();
  }

  @Test
  public void enableDisable() {
    final MethodLogger logger = new MethodLogger(10, false);
    assertFalse(logger.isEnabled());
    logger.logAccess("method");

    assertEquals(0, logger.getEntries().size());

    logger.setEnabled(true);
    logger.logAccess("method2");
    logger.logExit("method2");

    assertEquals(1, logger.getEntries().size());

    logger.setEnabled(false);
    assertEquals(0, logger.getEntries().size());
  }

  @Test
  public void singleLevelLogging() {
    final MethodLogger logger = new MethodLogger(10, false);
    logger.setEnabled(true);
    logger.logAccess("method");
    logger.logExit("method");

    assertEquals(1, logger.getEntries().size());
    final MethodLogger.Entry entry = logger.getEntries().get(0);
    assertEquals("method", entry.getMethod());

    logger.logAccess("method2");
    logger.logExit("method2");

    assertEquals(2, logger.getEntries().size());
    final MethodLogger.Entry entry2 = logger.getEntries().get(1);
    assertEquals("method2", entry2.getMethod());

    assertTrue(logger.getEntries().containsAll(Arrays.asList(entry, entry2)));
  }

  @Test
  public void twoLevelLogging() {
    final MethodLogger logger = new MethodLogger(10, false);
    logger.setEnabled(true);
    logger.logAccess("method", new Object[] {"param1", "param2"});
    logger.logAccess("subMethod");
    logger.logExit("subMethod");
    logger.logAccess("subMethod2");
    logger.logExit("subMethod2");
    logger.logExit("method");

    assertEquals(1, logger.getEntries().size());

    final MethodLogger.Entry lastEntry = logger.getEntries().get(0);
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
    final MethodLogger logger = new MethodLogger(10, false);
    logger.setEnabled(true);
    logger.logAccess("method");
    logger.logAccess("method");
    logger.logExit("method");
    logger.logAccess("method");
    logger.logExit("method");
    logger.logExit("method");

    assertEquals(1, logger.getEntries().size());

    final MethodLogger.Entry lastEntry = logger.getEntries().get(0);
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
    final MethodLogger logger = new MethodLogger(10, false);
    logger.setEnabled(true);
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

    assertEquals(1, logger.getEntries().size());

    final MethodLogger.Entry entry = logger.getEntries().get(0);
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

  @Test
  public void exitBeforeAccess() {
    final MethodLogger logger = new MethodLogger(10, false);
    logger.setEnabled(true);
    logger.logAccess("method");
    logger.logExit("method");
    assertThrows(IllegalStateException.class, () -> logger.logExit("method"));
  }

  @Test
  public void wrongMethodName() {
    final MethodLogger logger = new MethodLogger(10, false);
    logger.setEnabled(true);
    logger.logAccess("method");
    assertThrows(IllegalStateException.class, () -> logger.logExit("anotherMethod"));
  }

  @Test
  public void appendLogEntry() {
    final MethodLogger logger = new MethodLogger(10, false);
    logger.setEnabled(true);
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
    MethodLogger.appendLogEntry(new StringBuilder(), logger.getEntries().get(0), 0);
  }
}
