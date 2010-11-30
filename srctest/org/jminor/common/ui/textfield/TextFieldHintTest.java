package org.jminor.common.ui.textfield;

import org.junit.Test;

import javax.swing.JTextField;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TextFieldHintTest {

  @Test
  public void test() {
    final JTextField txt = new JTextField();
    try {
      TextFieldHint.enable(null, "test");
      fail("Null text field should not be accepted");
    }
    catch (Exception e) {}
    try {
      TextFieldHint.enable(txt, "");
      fail("Empty text field hint should not be accepted");
    }
    catch (Exception e) {}
    try {
      TextFieldHint.enable(txt, "test", null);
      fail("Null hint foreground color should not be accepted");
    }
    catch (Exception e) {}
    final TextFieldHint hint = TextFieldHint.enable(txt, "search");
    assertEquals("search", hint.getHintText());
    assertEquals("search", txt.getText());
    txt.setText("he");
    assertEquals("he", txt.getText());
  }
}
