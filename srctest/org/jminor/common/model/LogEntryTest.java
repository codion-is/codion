package org.jminor.common.model;

import static org.junit.Assert.*;
import org.junit.Test;

import java.util.Arrays;

public class LogEntryTest {

  @Test
  public void test() {
    final LogEntry entry = new LogEntry("method", "message", 1000, 1000000000, new Exception());
    assertFalse(entry.isComplete());
    assertEquals(Long.valueOf(1000).hashCode(), entry.hashCode());

    entry.setExitMessage("exit");
    entry.setExitTimeNano(1200000000);
    assertTrue(entry.isComplete());

    final LogEntry newEntry = new LogEntry("method", "message", 1100, 1100000000, new Exception());
    newEntry.setExitMessage("exit");
    entry.setSubLog(Arrays.asList(newEntry));

    assertEquals("method", entry.getMethod());
    assertEquals("message", entry.getEntryMessage());
    assertEquals(1000, entry.getEntryTime());
    assertEquals(1200, entry.getExitTime());
    assertNotNull(entry.getEntryTimeFormatted());
    assertNotNull(entry.getExitTimeFormatted());
    assertEquals(200, entry.getDelta());
    assertNotNull(entry.toString());

    final LogEntry copy = new LogEntry(entry);
    assertEquals(entry, copy);
    assertEquals(0, entry.compareTo(copy));

    copy.reset();
    assertNull(copy.getMethod());
    assertNull(copy.getEntryMessage());
    assertNull(copy.getExitMessage());
    assertEquals(0,copy.getEntryTime());
    assertEquals(0, copy.getExitTime());
    assertEquals(0, copy.getDelta());

    new LogEntry().getStackTrace();

    assertTrue(entry.getSubLog().contains(newEntry));
    assertNotNull(newEntry.toString());
    assertEquals(-1, entry.compareTo(newEntry));
    newEntry.set("method", "message", 900, 900000000, new Exception());
    assertEquals(1, entry.compareTo(newEntry));
  }
}
