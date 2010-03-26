/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.junit.Test;

public class CancelExceptionTest {

  @Test(expected=CancelException.class)
  public void test() throws CancelException {
    throw new CancelException();
  }
}
