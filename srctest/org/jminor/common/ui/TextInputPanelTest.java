package org.jminor.common.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

import javax.swing.JTextField;

public class TextInputPanelTest {

  @Test
  public void test() {
    final JTextField txtField = new JTextField();
    final TextInputPanel panel = new TextInputPanel(txtField, "title");
    assertEquals(txtField, panel.getTextComponent());
    assertNotNull(panel.getButton());
    panel.setMaxLength(10);
    assertEquals(10, panel.getMaxLength());
    txtField.setText("hello");
    assertEquals("hello", panel.getText());
    panel.setText("just");
    assertEquals("just", txtField.getText());
  }
}
