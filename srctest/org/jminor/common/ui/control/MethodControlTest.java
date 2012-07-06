/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.EventListener;
import org.jminor.common.model.State;
import org.jminor.common.model.States;

import org.junit.Test;

import javax.swing.JButton;
import java.awt.event.ActionEvent;

import static org.junit.Assert.*;

public class MethodControlTest {

  private int callCount = 0;
  private int actionPerformedCount = 0;

  public void method() {
    callCount++;
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
    final EventListener listener = new EventListener() {
      @Override
      public void eventOccurred(final ActionEvent e) {
        actionPerformedCount++;
      }
    };
    control.addActionPerformedListener(listener);
    control.actionPerformed(null);
    assertEquals("Action performed should have resulted in a method call", 2, callCount);
    assertEquals("Action performed should have resulted in a action performed count", 1, actionPerformedCount);
    control.removeActionPerformedListener(listener);
    try {
      new MethodControl("test", this, "none");
      fail();
    }
    catch (Exception e) {}
    new MethodControl("test", this, "method");
  }
}
