/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.jminor.common.EventListener;
import org.jminor.common.State;
import org.jminor.common.States;
import org.jminor.common.model.CancelException;

import org.junit.Test;

import javax.swing.JButton;

import static org.junit.Assert.*;

public final class MethodControlTest {

  private int callCount = 0;
  private int actionPerformedCount = 0;

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

  @Test(expected = IllegalArgumentException.class)
  public void constructorMethodNotFound() {
    new MethodControl("test", this, "none");
  }

  @Test
  public void test() throws Exception {
    final State stEnabled = States.state();
    final MethodControl control = new MethodControl("test", this, "method", stEnabled);
    final JButton btn = ControlProvider.createButton(control);
    assertFalse("Button should be disabled", btn.isEnabled());
    stEnabled.setActive(true);
    assertTrue("Button should be enabled", btn.isEnabled());
    btn.doClick();
    assertEquals("Button click should have resulted in a method call", 1, callCount);
    final EventListener listener = () -> actionPerformedCount++;
    control.addActionPerformedListener(listener);
    control.actionPerformed(null);
    assertEquals("Action performed should have resulted in a method call", 2, callCount);
    assertEquals("Action performed should have resulted in a action performed count", 1, actionPerformedCount);
    control.removeActionPerformedListener(listener);
    new MethodControl("test", this, "method");
  }

  @Test(expected = RuntimeException.class)
  public void exceptionOnExecute() {
    final MethodControl control = new MethodControl("test", this, "errorMethod", null);
    control.actionPerformed(null);
  }

  @Test(expected = RuntimeException.class)
  public void runtimeExceptionOnExecute() {
    final MethodControl control = new MethodControl("test", this, "runtimeErrorMethod", null);
    control.actionPerformed(null);
  }

  @Test
  public void cancelOnExecute() {
    final MethodControl control = new MethodControl("test", this, "cancelMethod", null);
    control.actionPerformed(null);
  }
}
