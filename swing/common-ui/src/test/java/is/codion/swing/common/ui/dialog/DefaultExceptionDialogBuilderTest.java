/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2014 - 2024, Björn Darri Sigurðsson.
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
