/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RecordModifiedExceptionTest {

  @Test
  public void test() {
    final RecordModifiedException ex = new RecordModifiedException("row", "hello", "message");
    assertEquals("row", ex.getRow());
    assertEquals("hello", ex.getModifiedRow());
    assertEquals("message", ex.getMessage());
  }
}