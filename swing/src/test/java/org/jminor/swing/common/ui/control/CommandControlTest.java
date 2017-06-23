/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.jminor.common.State;
import org.jminor.common.States;
import org.jminor.common.model.CancelException;

import org.junit.Test;

import javax.swing.JButton;

import static org.junit.Assert.*;

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
    final State stEnabled = States.state();
    final Control control = Controls.control(this::method, "test", stEnabled);
    final JButton btn = ControlProvider.createButton(control);
    assertFalse("Button should be disabled", btn.isEnabled());
    stEnabled.setActive(true);
    assertTrue("Button should be enabled", btn.isEnabled());
    btn.doClick();
    assertEquals("Button click should have resulted in a method call", 1, callCount);
  }

  @Test(expected = RuntimeException.class)
  public void exceptionOnExecute() {
    final Control control = Controls.control(this::errorMethod, "test", null);
    control.actionPerformed(null);
  }

  @Test(expected = RuntimeException.class)
  public void runtimeExceptionOnExecute() {
    final Control control = Controls.control(this::runtimeErrorMethod,"test", null);
    control.actionPerformed(null);
  }

  @Test
  public void cancelOnExecute() {
    final Control control = Controls.control(this::cancelMethod, "test", null);
    control.actionPerformed(null);
  }
}
