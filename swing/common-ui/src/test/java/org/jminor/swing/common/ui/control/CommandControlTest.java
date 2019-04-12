/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.jminor.common.State;
import org.jminor.common.States;
import org.jminor.common.model.CancelException;

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
    final Control control = Controls.control(this::method, "test", enabledState);
    final JButton button = new JButton(control);
    assertFalse(button.isEnabled());
    enabledState.setActive(true);
    assertTrue(button.isEnabled());
    button.doClick();
    assertEquals(1, callCount);
  }

  @Test
  public void exceptionOnExecute() {
    final Control control = Controls.control(this::errorMethod, "test", null);
    assertThrows(RuntimeException.class, () -> control.actionPerformed(null));
  }

  @Test
  public void runtimeExceptionOnExecute() {
    final Control control = Controls.control(this::runtimeErrorMethod, "test", null);
    assertThrows(RuntimeException.class, () -> control.actionPerformed(null));
  }

  @Test
  public void cancelOnExecute() {
    final Control control = Controls.control(this::cancelMethod, "test", null);
    control.actionPerformed(null);
  }
}
