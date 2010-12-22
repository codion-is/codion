package org.jminor.common.ui.textfield;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class IntFieldTest {

  @Test
  public void test() {
    final IntField txt = new IntField();
    txt.setInt(42);
    assertEquals("42", txt.getText());
    txt.setText("22");
    assertEquals(Integer.valueOf(22), txt.getInt());
    assertEquals(Integer.valueOf(22), txt.getValue());

    txt.setInt(10000000);
    assertEquals("10000000", txt.getText());
    txt.setInt(100000000);
    assertEquals("100000000", txt.getText());
  }
}
