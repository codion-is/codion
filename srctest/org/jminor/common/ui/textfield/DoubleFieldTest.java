package org.jminor.common.ui.textfield;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class DoubleFieldTest {

  @Test
  public void test() {
    final DoubleField txt = new DoubleField();
    txt.setDouble(42.2);
    assertEquals("42,2", txt.getText());
    txt.setText("22,3");
    assertEquals(Double.valueOf(22.3), txt.getDouble());
    txt.setText("22.3");
    assertEquals(Double.valueOf(22.3), txt.getValue());

    txt.setDecimalSymbol(".");
    txt.setDouble(42.2);
    assertEquals("42.2", txt.getText());
  }
}
