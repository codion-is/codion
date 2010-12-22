package org.jminor.common.ui.textfield;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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

    txt.setDouble(10000000d);
    assertEquals("10000000", txt.getText());
    txt.setDouble(100000000.4d);
    assertEquals("100000000.4", txt.getText());
  }
}
