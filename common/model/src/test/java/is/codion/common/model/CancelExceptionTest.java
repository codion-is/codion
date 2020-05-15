/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.common.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class CancelExceptionTest {

  @Test
  public void test() throws CancelException {
    assertThrows(CancelException.class, () -> {throw new CancelException();});
  }
}
