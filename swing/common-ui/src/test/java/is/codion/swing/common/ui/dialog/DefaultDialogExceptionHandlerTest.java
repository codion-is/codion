/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.model.CancelException;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DefaultDialogExceptionHandlerTest {

  @Test
  public void test() {
    Exception rootException = new Exception();
    RuntimeException wrapper = new RuntimeException(rootException);
    final List<Class<? extends Throwable>> toUnwrap = new ArrayList<>();
    toUnwrap.add(RuntimeException.class);
    Throwable unwrapped = DefaultDialogExceptionHandler.unwrapExceptions(wrapper, toUnwrap);
    assertEquals(rootException, unwrapped);

    rootException = new Exception();
    wrapper = new RuntimeException(new RuntimeException(rootException));
    unwrapped = DefaultDialogExceptionHandler.unwrapExceptions(wrapper, toUnwrap);
    assertEquals(rootException, unwrapped);

    rootException = new CancelException();
    wrapper = new RuntimeException(rootException);
    unwrapped = DefaultDialogExceptionHandler.unwrapExceptions(wrapper, toUnwrap);
    assertEquals(rootException, unwrapped);

    rootException = new Exception();
    unwrapped = DefaultDialogExceptionHandler.unwrapExceptions(rootException, toUnwrap);
    assertEquals(rootException, unwrapped);

    rootException = new RuntimeException();
    unwrapped = DefaultDialogExceptionHandler.unwrapExceptions(rootException, toUnwrap);
    assertEquals(rootException, unwrapped);
  }
}
