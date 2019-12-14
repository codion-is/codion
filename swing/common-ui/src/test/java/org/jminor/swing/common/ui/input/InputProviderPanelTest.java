/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.input;

import org.jminor.common.event.EventDataListener;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class InputProviderPanelTest {

  @Test
  public void test() {
    final JTextField textField = new JTextField();
    final InputProviderPanel panel = new InputProviderPanel("Test", new InputProvider<String, JTextField>() {
      @Override
      public JTextField getInputComponent() {
        return textField;
      }
      @Override
      public String getValue() {
        return textField.getText();
      }
      @Override
      public void setValue(final String value) {
        textField.setText(value);
      }
    });
    assertEquals(textField, panel.getInputComponent());
    textField.setText("hello");
    assertEquals("hello", panel.getValue());
    assertFalse(panel.isInputAccepted());
    final AtomicInteger eventCounter = new AtomicInteger();
    final EventDataListener<Integer> listener = data -> eventCounter.incrementAndGet();
    panel.addButtonClickListener(listener);
    panel.getOkButton().doClick();
    assertTrue(panel.isInputAccepted());
    assertEquals(1, eventCounter.get());
    panel.getCancelButton().doClick();
    assertEquals(2, eventCounter.get());
    panel.removeButtonClickListener(listener);
  }
}
