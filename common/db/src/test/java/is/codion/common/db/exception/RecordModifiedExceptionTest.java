/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RecordModifiedExceptionTest {

  @Test
  void test() {
    RecordModifiedException ex = new RecordModifiedException("row", "hello", "message");
    assertEquals("row", ex.row());
    assertEquals("hello", ex.modifiedRow());
    assertEquals("message", ex.getMessage());
  }
}