package org.jminor.common.db.exception;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class RecordModifiedExceptionTest {

  @Test
  public void test() {
    final RecordModifiedException ex = new RecordModifiedException("row", "hello");
    assertEquals("row", ex.getRow());
    assertEquals("hello", ex.getModifiedRow());
  }
}