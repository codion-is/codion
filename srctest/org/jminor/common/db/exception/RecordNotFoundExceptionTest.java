package org.jminor.common.db.exception;

import org.junit.Test;

public class RecordNotFoundExceptionTest {

  @Test
  public void test() {
    new RecordNotFoundException("hello");
  }
}
