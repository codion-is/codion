/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.common.event.EventDataListener;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ComponentValuePanelTest {

  @Test
  public void componentValuePanel() {
    final JTextField textField = new JTextField();
    final ComponentValuePanel panel = new ComponentValuePanel("Test", new AbstractComponentValue<String, JTextField>(textField) {
      @Override
      protected String getComponentValue(final JTextField component) {
        return component.getText();
      }
      @Override
      protected void setComponentValue(final JTextField component, final String value) {
        component.setText(value);
      }
    });
    assertEquals(textField, panel.getInputComponent());
    textField.setText("hello");
    assertEquals("hello", panel.getValue());
    assertFalse(panel.isInputAccepted());
    final AtomicInteger eventCounter = new AtomicInteger();
    final EventDataListener<Integer> listener = data -> eventCounter.incrementAndGet();
    panel.addButtonClickListener(listener);
    panel.getOkAction().actionPerformed(null);
    assertTrue(panel.isInputAccepted());
    assertEquals(1, eventCounter.get());
    panel.getCancelAction().actionPerformed(null);
    assertEquals(2, eventCounter.get());
    panel.removeButtonClickListener(listener);
  }
}
