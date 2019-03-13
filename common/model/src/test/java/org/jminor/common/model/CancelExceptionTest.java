/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class CancelExceptionTest {

  @Test
  public void test() throws CancelException {
    assertThrows(CancelException.class, () -> {throw new CancelException();});
  }
}
