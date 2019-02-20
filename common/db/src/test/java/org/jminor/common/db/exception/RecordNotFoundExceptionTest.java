/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class RecordNotFoundExceptionTest {

  @Test
  public void test() {
    assertThrows(RecordNotFoundException.class, () -> {
      throw new RecordNotFoundException("hello");
    });
  }
}
