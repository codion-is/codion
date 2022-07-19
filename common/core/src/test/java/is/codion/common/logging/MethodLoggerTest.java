/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.logging;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class MethodLoggerTest {

  @Test
  void test() throws Exception {
    MethodLogger logger = MethodLogger.methodLogger(10);
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
  void serialize() throws IOException, ClassNotFoundException {
    MethodLogger logger = MethodLogger.methodLogger(10);
    logger.setEnabled(true);
    logger.logAccess("method");
    logger.logAccess("method2");
    logger.logExit("method2");
    logger.logExit("method");

    ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
    new ObjectOutputStream(byteOut).writeObject(logger.getEntries());
    new ObjectInputStream(new ByteArrayInputStream(byteOut.toByteArray())).readObject();
  }

  @Test
  void enableDisable() {
    MethodLogger logger = MethodLogger.methodLogger(10);
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
  void singleLevelLogging() {
    MethodLogger logger = MethodLogger.methodLogger(10);
    logger.setEnabled(true);
    logger.logAccess("method");
    logger.logExit("method");

    assertEquals(1, logger.getEntries().size());
    MethodLogger.Entry entry = logger.getEntries().get(0);
    assertEquals("method", entry.getMethod());

    logger.logAccess("method2");
    logger.logExit("method2");

    assertEquals(2, logger.getEntries().size());
    MethodLogger.Entry entry2 = logger.getEntries().get(1);
    assertEquals("method2", entry2.getMethod());

    assertTrue(logger.getEntries().containsAll(asList(entry, entry2)));
  }

  @Test
  void twoLevelLogging() {
    MethodLogger logger = MethodLogger.methodLogger(10);
    logger.setEnabled(true);
    logger.logAccess("method", new Object[] {"param1", "param2"});
    logger.logAccess("subMethod");
    logger.logExit("subMethod");
    logger.logAccess("subMethod2");
    logger.logExit("subMethod2");
    logger.logExit("method");

    assertEquals(1, logger.getEntries().size());

    MethodLogger.Entry lastEntry = logger.getEntries().get(0);
    assertEquals("method", lastEntry.getMethod());
    assertTrue(lastEntry.hasChildEntries());
    List<MethodLogger.Entry> subLog = lastEntry.getChildEntries();
    assertEquals(2, subLog.size());
    MethodLogger.Entry subEntry = subLog.get(0);
    assertEquals("subMethod", subEntry.getMethod());
    assertFalse(subEntry.hasChildEntries());
  }

  @Test
  void twoLevelLoggingSameMethodName() {
    MethodLogger logger = MethodLogger.methodLogger(10);
    logger.setEnabled(true);
    logger.logAccess("method");
    logger.logAccess("method");
    logger.logExit("method");
    logger.logAccess("method");
    logger.logExit("method");
    logger.logExit("method");

    assertEquals(1, logger.getEntries().size());

    MethodLogger.Entry lastEntry = logger.getEntries().get(0);
    assertEquals("method", lastEntry.getMethod());
    assertTrue(lastEntry.hasChildEntries());
    List<MethodLogger.Entry> subLog = lastEntry.getChildEntries();
    assertEquals(2, subLog.size());
    MethodLogger.Entry subEntry = subLog.get(0);
    assertEquals("method", subEntry.getMethod());
    assertFalse(subEntry.hasChildEntries());
  }

  @Test
  void threeLevelLogging() {
    MethodLogger logger = MethodLogger.methodLogger(10);
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

    MethodLogger.Entry entry = logger.getEntries().get(0);
    assertEquals("one", entry.getMethod());
    assertTrue(entry.hasChildEntries());
    List<MethodLogger.Entry> subLog = entry.getChildEntries();
    assertEquals(2, subLog.size());
    MethodLogger.Entry subEntry1 = subLog.get(0);
    assertEquals("two", subEntry1.getMethod());
    assertTrue(entry.hasChildEntries());
    MethodLogger.Entry subEntry2 = subLog.get(1);
    assertEquals("two2", subEntry2.getMethod());
    assertTrue(entry.hasChildEntries());

    List<MethodLogger.Entry> subSubLog = subEntry1.getChildEntries();
    MethodLogger.Entry subSubEntry = subSubLog.get(0);
    assertEquals("three", subSubEntry.getMethod());
    assertFalse(subSubEntry.hasChildEntries());
    List<MethodLogger.Entry> subSubLog2 = subEntry2.getChildEntries();
    MethodLogger.Entry subSubEntry2 = subSubLog2.get(0);
    assertEquals("three2", subSubEntry2.getMethod());
    assertFalse(subSubEntry2.hasChildEntries());
  }

  @Test
  void exitBeforeAccess() {
    MethodLogger logger = MethodLogger.methodLogger(10);
    logger.setEnabled(true);
    logger.logAccess("method");
    logger.logExit("method");
    assertThrows(IllegalStateException.class, () -> logger.logExit("method"));
  }

  @Test
  void wrongMethodName() {
    MethodLogger logger = MethodLogger.methodLogger(10);
    logger.setEnabled(true);
    logger.logAccess("method");
    assertThrows(IllegalStateException.class, () -> logger.logExit("anotherMethod"));
  }

  @Test
  void appendLogEntry() {
    MethodLogger logger = MethodLogger.methodLogger(10);
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
    logger.getEntries().get(0).append(new StringBuilder());
  }
}
