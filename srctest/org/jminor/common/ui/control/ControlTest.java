package org.jminor.common.ui.control;

import org.jminor.common.model.State;
import org.jminor.common.model.States;

import static org.junit.Assert.*;
import org.junit.Test;

public class ControlTest {

  @Test
  public void test() throws Exception {
    final Control test = new Control();
    test.setName("test");
    assertEquals("test", test.getName());
    test.setMnemonic(10);
    assertEquals(10, test.getMnemonic());
    assertNull(test.getIcon());
    test.setKeyStroke(null);

    final State enabledState = States.state();
    final Control control = new Control("control", enabledState);
    assertEquals("control", control.getName());
    assertEquals(enabledState, control.getEnabledState());
    assertFalse(control.isEnabled());
    enabledState.setActive(true);
    assertTrue(control.isEnabled());

    control.setDescription("description");
    assertEquals("description", control.getDescription());

    enabledState.setActive(false);
    
    assertFalse(control.isEnabled());
  }
}
