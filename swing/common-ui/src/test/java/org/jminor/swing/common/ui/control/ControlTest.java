/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.jminor.common.state.State;
import org.jminor.common.state.States;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ControlTest {

  @Test
  public void test() throws Exception {
    final Control test = new Control();
    test.setName("test");
    assertEquals("test", test.toString());
    assertEquals("test", test.getName());
    assertEquals(0, test.getMnemonic());
    test.setMnemonic(10);
    assertEquals(10, test.getMnemonic());
    assertNull(test.getIcon());
    test.setKeyStroke(null);
    test.setDescription("description");
    assertEquals("description", test.getDescription());
    test.actionPerformed(null);
  }

  @Test
  public void setEnabled() {
    final State enabledState = States.state();
    final Control control = new Control("control", enabledState);
    assertEquals("control", control.getName());
    assertEquals(enabledState, control.getEnabledObserver());
    assertFalse(control.isEnabled());
    enabledState.set(true);
    assertTrue(control.isEnabled());
    enabledState.set(false);
    assertFalse(control.isEnabled());
  }

  @Test
  public void setEnabledViaMethod() {
    final Control test = new Control();
    assertThrows(UnsupportedOperationException.class, () -> test.setEnabled(true));
  }
}
