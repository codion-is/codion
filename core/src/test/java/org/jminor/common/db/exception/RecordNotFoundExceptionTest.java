/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.exception;

import org.junit.Test;

public class RecordNotFoundExceptionTest {

  @Test
  public void test() {
    new RecordNotFoundException("hello");
  }
}
