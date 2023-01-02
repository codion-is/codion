/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class CancelExceptionTest {

  @Test
  void test() throws CancelException {
    assertThrows(CancelException.class, () -> {throw new CancelException();});
  }
}
