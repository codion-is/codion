/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.State;

import static org.junit.Assert.*;
import org.junit.Test;

import javax.swing.JButton;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * User: Björn Darri
 * Date: 13.1.2008
 * Time: 13:05:09
 */
public class MethodControlTest {

  private int callCount = 0;
  private int actionPerformedCount = 0;

  public void method() {
    callCount++;
  }

  @Test
  public void test() throws Exception {
    final State stEnabled = new State();
    final MethodControl control = new MethodControl("test", this, "method", stEnabled);
    final JButton btn = ControlProvider.createButton(control);
    assertFalse("Button should be disabled", btn.isEnabled());
    stEnabled.setActive(true);
    assertTrue("Button should be enabled", btn.isEnabled());
    btn.doClick();
    assertEquals("Button click should have resulted in a method call", 1, callCount);
    control.eventActionPerformed().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        actionPerformedCount++;
      }
    });
    control.actionPerformed(null);
    assertEquals("Action performed should have resulted in a method call", 2, callCount);
    assertEquals("Action performed should have resulted in a action performed count", 1, actionPerformedCount);
    try {
      new MethodControl("test", this, "none", null);
      fail();
    }
    catch (Exception e) {}
  }
}
