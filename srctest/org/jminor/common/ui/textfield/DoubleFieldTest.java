package org.jminor.common.ui.textfield;

import org.junit.Test;

import javax.swing.text.BadLocationException;

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
    assertEquals(Double.valueOf(22.3), txt.getDouble());

    txt.setDecimalSymbol(".");
    txt.setDouble(42.2);
    assertEquals("42.2", txt.getText());

    txt.setDouble(10000000d);
    assertEquals("10000000", txt.getText());
    txt.setDouble(100000000.4d);
    assertEquals("100000000.4", txt.getText());
  }

  @Test(expected = IllegalArgumentException.class)
  public void setMaximumFractionDigitsToZero() {
    new DoubleField().setMaximumFractionDigits(0);
  }

  @Test
  public void maximumFractionDigits() throws BadLocationException {
    final DoubleField txt = new DoubleField();
    assertEquals(-1, txt.getMaximumFractionDigits());
    txt.setMaximumFractionDigits(2);
    txt.setDouble(5.1254);
    assertEquals("5,12", txt.getText());
    txt.setText("5,123");
    assertEquals("5,12", txt.getText());
    txt.getDocument().insertString(3, "4", null);
    assertEquals("5,14", txt.getText());
    txt.setMaximumFractionDigits(3);
    txt.setText("5,12378");
    assertEquals("5,123", txt.getText());//no rounding should occur
    txt.setMaximumFractionDigits(-1);
    txt.setText("5,12378");
    assertEquals("5,12378", txt.getText());
  }
}
