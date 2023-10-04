/*
 * Copyright (c) 2014 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.model.CancelException;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DefaultExceptionDialogBuilderTest {

  @Test
  void test() {
    Exception rootException = new Exception();
    RuntimeException wrapper = new RuntimeException(rootException);
    List<Class<? extends Throwable>> toUnwrap = new ArrayList<>();
    toUnwrap.add(RuntimeException.class);
    Throwable unwrapped = DefaultExceptionDialogBuilder.unwrapExceptions(wrapper, toUnwrap);
    assertEquals(rootException, unwrapped);

    rootException = new Exception();
    wrapper = new RuntimeException(new RuntimeException(rootException));
    unwrapped = DefaultExceptionDialogBuilder.unwrapExceptions(wrapper, toUnwrap);
    assertEquals(rootException, unwrapped);

    rootException = new CancelException();
    wrapper = new RuntimeException(rootException);
    unwrapped = DefaultExceptionDialogBuilder.unwrapExceptions(wrapper, toUnwrap);
    assertEquals(rootException, unwrapped);

    rootException = new Exception();
    unwrapped = DefaultExceptionDialogBuilder.unwrapExceptions(rootException, toUnwrap);
    assertEquals(rootException, unwrapped);

    rootException = new RuntimeException();
    unwrapped = DefaultExceptionDialogBuilder.unwrapExceptions(rootException, toUnwrap);
    assertEquals(rootException, unwrapped);
  }
}
