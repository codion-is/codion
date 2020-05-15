/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.model.CancelException;
import is.codion.common.state.State;
import is.codion.common.state.States;

import org.junit.jupiter.api.Test;

import javax.swing.JButton;

import static org.junit.jupiter.api.Assertions.*;

public final class CommandControlTest {

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
    final State enabledState = States.state();
    final Control control = Controls.control(this::method, enabledState);
    final JButton button = new JButton(control);
    assertFalse(button.isEnabled());
    enabledState.set(true);
    assertTrue(button.isEnabled());
    button.doClick();
    assertEquals(1, callCount);
  }

  @Test
  public void exceptionOnExecute() {
    final Control control = Controls.control(this::errorMethod);
    assertThrows(RuntimeException.class, () -> control.actionPerformed(null));
  }

  @Test
  public void runtimeExceptionOnExecute() {
    final Control control = Controls.control(this::runtimeErrorMethod);
    assertThrows(RuntimeException.class, () -> control.actionPerformed(null));
  }

  @Test
  public void cancelOnExecute() {
    final Control control = Controls.control(this::cancelMethod);
    control.actionPerformed(null);
  }
}
