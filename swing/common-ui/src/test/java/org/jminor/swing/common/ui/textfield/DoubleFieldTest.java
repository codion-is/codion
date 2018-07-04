/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.textfield;

import org.junit.jupiter.api.Test;

import javax.swing.text.BadLocationException;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DoubleFieldTest {

  @Test
  public void testNoGrouping() {
    final DoubleField txt = new DoubleField();
    txt.setSeparators(',', '.');
    txt.setText(",");
    assertEquals("0,", txt.getText());
    txt.setDouble(42.2);
    assertEquals("42,2", txt.getText());
    txt.setText("22,3");
    assertEquals(Double.valueOf(22.3), txt.getDouble());
    txt.setText("22.5");//note this is a thousand separator
    assertEquals(Double.valueOf(22.3), txt.getDouble());
    assertEquals("22,3", txt.getText());
    txt.setText("22.123.123,123");
    assertEquals("22,3", txt.getText());
    assertEquals(Double.valueOf(22.3), txt.getDouble());

    txt.setSeparators('.', ',');

    txt.setDouble(42.2);
    assertEquals("42.2", txt.getText());
    txt.setText("2,123,123.123");
    assertEquals("42.2", txt.getText());
    assertEquals(Double.valueOf(42.2), txt.getDouble());

    txt.setDouble(10000000d);
    assertEquals("10000000", txt.getText());
    txt.setDouble(100000000.4d);
    assertEquals("100000000.4", txt.getText());
  }

  @Test
  public void testGrouping() {
    final DoubleField txt = new DoubleField();
    txt.setGroupingUsed(true);
    txt.setSeparators(',', '.');
    assertEquals(0, txt.getCaretPosition());
    txt.setText(",");
    assertEquals("0,", txt.getText());
    assertEquals(2, txt.getCaretPosition());
    txt.setDouble(42.2);
    assertEquals("42,2", txt.getText());
    assertEquals(4, txt.getCaretPosition());
    txt.setText("22,3");
    assertEquals(Double.valueOf(22.3), txt.getDouble());
    txt.setText("22.3");//note this is a thousand separator
    assertEquals(Double.valueOf(223), txt.getDouble());
    assertEquals("223", txt.getText());
    txt.setText("22.123.123,123");
    assertEquals("22.123.123,123", txt.getText());
    assertEquals(Double.valueOf(22123123.123), txt.getDouble());
    txt.setText("22123123,123");
    assertEquals("22.123.123,123", txt.getText());
    assertEquals(Double.valueOf(22123123.123), txt.getDouble());

    txt.setSeparators('.', ',');

    txt.setDouble(42.2);
    assertEquals("42.2", txt.getText());
    txt.setText("22,123,123.123");
    assertEquals("22,123,123.123", txt.getText());
    assertEquals(Double.valueOf(22123123.123), txt.getDouble());
    txt.setText("22123123.123");
    assertEquals("22,123,123.123", txt.getText());
    assertEquals(Double.valueOf(22123123.123), txt.getDouble());

    txt.setDouble(10000000d);
    assertEquals("10,000,000", txt.getText());
    txt.setDouble(100000000.4d);
    assertEquals("100,000,000.4", txt.getText());

    txt.setText("2.2.2");
    assertEquals("100,000,000.4", txt.getText());
    txt.setText("..22.2.2.2");
    assertEquals("100,000,000.4", txt.getText());
    txt.setText("22.2.2.2");
    assertEquals("100,000,000.4", txt.getText());
    txt.setText("2222.2.2.2");
    assertEquals("100,000,000.4", txt.getText());
  }

  @Test
  public void caretPosition() throws BadLocationException {
    final DoubleField txt = new DoubleField();
    txt.setGroupingUsed(true);
    txt.setSeparators(',', '.');
    final NumberField.NumberDocument document = (NumberField.NumberDocument) txt.getDocument();

    txt.setText("123456789");
    assertEquals("123.456.789", txt.getText());

    txt.setCaretPosition(3);
    txt.moveCaretPosition(8);
    assertEquals(".456.", txt.getSelectedText());

    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(""), null);

    txt.paste();
    assertEquals("123.789", txt.getText());
    assertEquals(4, txt.getCaretPosition());

    document.insertString(3, "456", null);
    assertEquals("123.456.789", txt.getText());
    assertEquals(7, txt.getCaretPosition());

    txt.setCaretPosition(3);
    txt.moveCaretPosition(11);
    assertEquals(".456.789", txt.getSelectedText());

    txt.paste();
    assertEquals("123", txt.getText());
    assertEquals(3, txt.getCaretPosition());

    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(".456.789"), null);

    txt.paste();
    assertEquals("123.456.789", txt.getText());
    assertEquals(11, txt.getCaretPosition());

    txt.setText("");

    document.insertString(0, ",", null);
    assertEquals("0,", txt.getText());
    assertEquals(2, txt.getCaretPosition());

    txt.setText("");
    document.insertString(0, "1", null);
    assertEquals("1", txt.getText());
    assertEquals(1, txt.getCaretPosition());

    document.insertString(1, "2", null);
    assertEquals("12", txt.getText());
    assertEquals(2, txt.getCaretPosition());

    document.insertString(2, "3", null);
    assertEquals("123", txt.getText());
    assertEquals(3, txt.getCaretPosition());

    document.insertString(3, "4", null);
    assertEquals("1.234", txt.getText());
    assertEquals(5, txt.getCaretPosition());

    document.insertString(5, "5", null);
    assertEquals("12.345", txt.getText());
    assertEquals(6, txt.getCaretPosition());

    document.insertString(6, "6", null);
    assertEquals("123.456", txt.getText());
    assertEquals(7, txt.getCaretPosition());

    document.insertString(7, "7", null);
    assertEquals("1.234.567", txt.getText());
    assertEquals(9, txt.getCaretPosition());
  }

  @Test
  public void setMaximumFractionDigitsToZero() {
    assertThrows(IllegalArgumentException.class, () -> new DoubleField().setMaximumFractionDigits(0));
  }

  @Test
  public void setSeparatorsSameCharacter() {
    assertThrows(IllegalArgumentException.class, () -> new DoubleField().setSeparators('.', '.'));
  }

  @Test
  public void maximumFractionDigits() throws BadLocationException {
    final DoubleField txt = new DoubleField();
    assertEquals(-1, txt.getMaximumFractionDigits());
    txt.setSeparators(',', '.');
    txt.setMaximumFractionDigits(2);
    assertEquals(2, txt.getMaximumFractionDigits());
    txt.setDouble(5.1254);
    assertEquals("5,12", txt.getText());
    txt.setText("5,123");
    assertEquals("5,12", txt.getText());
    txt.getDocument().insertString(3, "4", null);
    assertEquals("5,14", txt.getText());
    txt.getDocument().remove(3, 1);
    assertEquals("5,1", txt.getText());
    txt.setMaximumFractionDigits(3);
    txt.setText("5,12378");
    assertEquals("5,123", txt.getText());//no rounding should occur
    txt.setMaximumFractionDigits(-1);
    txt.setText("5,12378");
    assertEquals("5,12378", txt.getText());
  }

  @Test
  public void decimalSeparators() {
    final DoubleField txt = new DoubleField();
    txt.setSeparators('.', ',');
    txt.setText("1.5");
    assertEquals(Double.valueOf(1.5), txt.getDouble());
    txt.setText("123.34.56");
    assertEquals(Double.valueOf(1.5), txt.getDouble());

    txt.setText("1,5");
    assertEquals(Double.valueOf(1.5), txt.getDouble());
    txt.setText("1,4.5");
    assertEquals(Double.valueOf(1.5), txt.getDouble());
  }

  @Test
  public void trailingDecimalSeparator() throws BadLocationException {
    final DoubleField txt = new DoubleField();
    txt.setSeparators('.', ',');
    final NumberField.NumberDocument document = (NumberField.NumberDocument) txt.getDocument();
    document.insertString(0, "1", null);
    assertEquals(Double.valueOf(1), txt.getDouble());
    document.insertString(1, ".", null);
    assertEquals("1.", txt.getText());
    assertEquals(Double.valueOf(1), txt.getDouble());
    document.insertString(2, "1", null);
    assertEquals("1.1", txt.getText());
    assertEquals(Double.valueOf(1.1), txt.getDouble());
  }

  @Test
  public void setSeparators() {
    final DoubleField txt = new DoubleField();
    txt.setGroupingUsed(true);
    txt.setSeparators('.', ',');
    txt.setNumber(12345678.9);
    assertEquals("12,345,678.9", txt.getText());
    txt.setSeparators(',', '.');
    assertEquals("12.345.678,9", txt.getText());
  }

  @Test
  public void trailingDecimalZeros() throws BadLocationException {
    final DoubleField txt = new DoubleField();
    final NumberField.NumberDocument document = (NumberField.NumberDocument) txt.getDocument();
    txt.setSeparators('.', ',');

    document.insertString(0, "1", null);
    document.insertString(1, ".", null);
    assertEquals("1.", txt.getText());

    document.insertString(2, "0", null);
    assertEquals("1.0", txt.getText());

    document.insertString(3, "0", null);
    assertEquals("1.00", txt.getText());

    document.insertString(4, "1", null);
    assertEquals("1.001", txt.getText());

    document.insertString(5, "0", null);
    assertEquals("1.0010", txt.getText());

    document.insertString(6, "0", null);
    assertEquals("1.00100", txt.getText());

    document.insertString(7, "2", null);
    assertEquals("1.001002", txt.getText());

    document.insertString(8, "0", null);
    assertEquals("1.0010020", txt.getText());

    txt.setText("");
    document.insertString(0, ".", null);
    assertEquals("0.", txt.getText());

    document.insertString(2, "0", null);
    assertEquals("0.0", txt.getText());

    document.insertString(3, "1", null);
    assertEquals("0.01", txt.getText());
  }
}
