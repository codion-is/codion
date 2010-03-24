package org.jminor.common.ui.control;

import org.jminor.common.model.State;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class ControlTest {

  @Test
  public void test() throws Exception {
    final State enabledState = new State();
    final Control control = new Control("control", enabledState);
    assertFalse(control.isEnabled());
    enabledState.setActive(true);
    assertTrue(control.isEnabled());

    control.setDescription("description");
    assertEquals("description", control.getDescription());

    control.setEnabled(false);
    assertFalse(enabledState.isActive());
    assertFalse(control.isEnabled());
  }
}
