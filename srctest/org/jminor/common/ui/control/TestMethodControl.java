/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.State;
import org.jminor.common.ui.ControlProvider;

import junit.framework.TestCase;

import javax.swing.JButton;

/**
 * User: Björn Darri
 * Date: 13.1.2008
 * Time: 13:05:09
 */
public class TestMethodControl extends TestCase {

  private int callCount = 0;

  public TestMethodControl() {
    super("TestMethodControl");
  }

  public void method() {
    callCount++;
  }

  public void test() throws Exception {
    final State stEnabled = new State();
    final Control control = new MethodControl("test", this, "method", stEnabled);
    final JButton btn = ControlProvider.createButton(control);
    assertFalse("Button should be disabled", btn.isEnabled());
    stEnabled.setActive(true);
    assertTrue("Button should be enabled", btn.isEnabled());
    btn.doClick();
    assertEquals("Button click should have resulted in a method call", 1, callCount);
  }
}
