/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.model.CancelException;
import is.codion.common.state.State;

import org.junit.jupiter.api.Test;

import javax.swing.JButton;

import static org.junit.jupiter.api.Assertions.*;

public final class DefaultControlTest {

  private int callCount = 0;

  public void method() {
    callCount++;
  }

  public void errorMethod() throws Exception {
    throw new Exception("test");
  }

  public void runtimeErrorMethod() {
    throw new RuntimeException("test");
  }

  public void cancelMethod() {
    throw new CancelException();
  }

  @Test
  public void test() throws Exception {
    final State enabledState = State.state();
    final Control control = Control.builder().command(this::method).enabledState(enabledState).build();
    final JButton button = control.createButton();
    assertFalse(button.isEnabled());
    enabledState.set(true);
    assertTrue(button.isEnabled());
    button.doClick();
    assertEquals(1, callCount);
  }

  @Test
  public void exceptionOnExecute() {
    final Control control = Control.control(this::errorMethod);
    assertThrows(RuntimeException.class, () -> control.actionPerformed(null));
  }

  @Test
  public void runtimeExceptionOnExecute() {
    final Control control = Control.control(this::runtimeErrorMethod);
    assertThrows(RuntimeException.class, () -> control.actionPerformed(null));
  }

  @Test
  public void cancelOnExecute() {
    final Control control = Control.control(this::cancelMethod);
    control.actionPerformed(null);
  }
}
