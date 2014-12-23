/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.model.CancelException;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class DefaultExceptionHandlerTest {

  @Test
  public void test() {
    Exception rootException = new Exception();
    RuntimeException wrapper = new RuntimeException(rootException);
    final List<Class<? extends Throwable>> toUnwrap = new ArrayList<>();
    toUnwrap.add(RuntimeException.class);
    Throwable unwrapped = DefaultExceptionHandler.unwrapExceptions(wrapper, toUnwrap);
    assertEquals(rootException, unwrapped);

    rootException = new Exception();
    wrapper = new RuntimeException(new RuntimeException(rootException));
    unwrapped = DefaultExceptionHandler.unwrapExceptions(wrapper, toUnwrap);
    assertEquals(rootException, unwrapped);

    rootException = new CancelException();
    wrapper = new RuntimeException(rootException);
    unwrapped = DefaultExceptionHandler.unwrapExceptions(wrapper, toUnwrap);
    assertEquals(rootException, unwrapped);

    rootException = new Exception();
    unwrapped = DefaultExceptionHandler.unwrapExceptions(rootException, toUnwrap);
    assertEquals(rootException, unwrapped);

    rootException = new RuntimeException();
    unwrapped = DefaultExceptionHandler.unwrapExceptions(rootException, toUnwrap);
    assertEquals(rootException, unwrapped);
  }
}
