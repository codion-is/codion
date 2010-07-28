package org.jminor.common.ui.textfield;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class TextFieldPlusTest {

  @Test
  public void test() {
    final TextFieldPlus txt = new TextFieldPlus();
    txt.setText("hello");
    assertEquals("hello", txt.getValue());

    txt.setMaxLength(10);
    assertEquals(10, txt.getMaxLength());

    txt.setText("hellohello");
    assertEquals("hellohello", txt.getValue());

    txt.setText("hellohellohello");//invalid
    assertEquals("", txt.getValue());

    txt.setUpperCase(true);
    assertTrue(txt.isUpperCase());

    txt.setText("hello");
    assertEquals("HELLO", txt.getText());

    txt.setRange(0, 10);
    assertEquals(0, (int) txt.getMinimumValue());
    assertEquals(10, (int) txt.getMaximumValue());
  }
}
