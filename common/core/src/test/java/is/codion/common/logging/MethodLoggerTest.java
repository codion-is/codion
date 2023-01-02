/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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

    assertEquals(1, logger.entries().size());
    logger.setEnabled(false);
    assertEquals(0, logger.entries().size());
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
    new ObjectOutputStream(byteOut).writeObject(logger.entries());
    new ObjectInputStream(new ByteArrayInputStream(byteOut.toByteArray())).readObject();
  }

  @Test
  void enableDisable() {
    MethodLogger logger = MethodLogger.methodLogger(10);
    assertFalse(logger.isEnabled());
    logger.logAccess("method");

    assertEquals(0, logger.entries().size());

    logger.setEnabled(true);
    logger.logAccess("method2");
    logger.logExit("method2");

    assertEquals(1, logger.entries().size());

    logger.setEnabled(false);
    assertEquals(0, logger.entries().size());
  }

  @Test
  void singleLevelLogging() {
    MethodLogger logger = MethodLogger.methodLogger(10);
    logger.setEnabled(true);
    logger.logAccess("method");
    logger.logExit("method");

    assertEquals(1, logger.entries().size());
    MethodLogger.Entry entry = logger.entries().get(0);
    assertEquals("method", entry.method());

    logger.logAccess("method2");
    logger.logExit("method2");

    assertEquals(2, logger.entries().size());
    MethodLogger.Entry entry2 = logger.entries().get(1);
    assertEquals("method2", entry2.method());

    assertTrue(logger.entries().containsAll(asList(entry, entry2)));
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

    assertEquals(1, logger.entries().size());

    MethodLogger.Entry lastEntry = logger.entries().get(0);
    assertEquals("method", lastEntry.method());
    assertTrue(lastEntry.hasChildEntries());
    List<MethodLogger.Entry> subLog = lastEntry.childEntries();
    assertEquals(2, subLog.size());
    MethodLogger.Entry subEntry = subLog.get(0);
    assertEquals("subMethod", subEntry.method());
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

    assertEquals(1, logger.entries().size());

    MethodLogger.Entry lastEntry = logger.entries().get(0);
    assertEquals("method", lastEntry.method());
    assertTrue(lastEntry.hasChildEntries());
    List<MethodLogger.Entry> subLog = lastEntry.childEntries();
    assertEquals(2, subLog.size());
    MethodLogger.Entry subEntry = subLog.get(0);
    assertEquals("method", subEntry.method());
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

    assertEquals(1, logger.entries().size());

    MethodLogger.Entry entry = logger.entries().get(0);
    assertEquals("one", entry.method());
    assertTrue(entry.hasChildEntries());
    List<MethodLogger.Entry> subLog = entry.childEntries();
    assertEquals(2, subLog.size());
    MethodLogger.Entry subEntry1 = subLog.get(0);
    assertEquals("two", subEntry1.method());
    assertTrue(entry.hasChildEntries());
    MethodLogger.Entry subEntry2 = subLog.get(1);
    assertEquals("two2", subEntry2.method());
    assertTrue(entry.hasChildEntries());

    List<MethodLogger.Entry> subSubLog = subEntry1.childEntries();
    MethodLogger.Entry subSubEntry = subSubLog.get(0);
    assertEquals("three", subSubEntry.method());
    assertFalse(subSubEntry.hasChildEntries());
    List<MethodLogger.Entry> subSubLog2 = subEntry2.childEntries();
    MethodLogger.Entry subSubEntry2 = subSubLog2.get(0);
    assertEquals("three2", subSubEntry2.method());
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
    logger.entries().get(0).append(new StringBuilder());
  }
}
