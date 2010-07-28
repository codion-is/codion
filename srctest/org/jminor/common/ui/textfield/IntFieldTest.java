package org.jminor.common.ui.textfield;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class IntFieldTest {

  @Test
  public void test() {
    final IntField txt = new IntField();
    txt.setInt(42);
    assertEquals("42", txt.getText());
    txt.setText("22");
    assertEquals(Integer.valueOf(22), txt.getInt());
    assertEquals(Integer.valueOf(22), txt.getValue());
  }
}
