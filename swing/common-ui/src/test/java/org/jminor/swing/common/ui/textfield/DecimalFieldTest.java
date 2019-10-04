/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.textfield;

import org.junit.jupiter.api.Test;

import javax.swing.text.BadLocationException;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DecimalFieldTest {

  @Test
  public void testNoGrouping() {
    final DecimalField decimalField = new DecimalField();
    decimalField.setSeparators(',', '.');
    decimalField.setText(",");
    assertEquals("0,", decimalField.getText());
    decimalField.setDouble(42.2);
    assertEquals("42,2", decimalField.getText());
    decimalField.setText("22,3");
    assertEquals(Double.valueOf(22.3), decimalField.getDouble());
    decimalField.setText("22.5");//note this is a thousand separator
    assertEquals(Double.valueOf(22.3), decimalField.getDouble());
    assertEquals("22,3", decimalField.getText());
    decimalField.setText("22.123.123,123");
    assertEquals("22,3", decimalField.getText());
    assertEquals(Double.valueOf(22.3), decimalField.getDouble());

    decimalField.setSeparators('.', ',');

    decimalField.setDouble(42.2);
    assertEquals("42.2", decimalField.getText());
    decimalField.setText("2,123,123.123");
    assertEquals("42.2", decimalField.getText());
    assertEquals(Double.valueOf(42.2), decimalField.getDouble());

    decimalField.setDouble(10000000d);
    assertEquals("10000000", decimalField.getText());
    decimalField.setDouble(100000000.4d);
    assertEquals("100000000.4", decimalField.getText());
  }

  @Test
  public void testGrouping() {
    final DecimalField decimalField = new DecimalField();
    decimalField.setGroupingUsed(true);
    decimalField.setSeparators(',', '.');
    assertEquals(0, decimalField.getCaretPosition());
    decimalField.setText(",");
    assertEquals("0,", decimalField.getText());
    assertEquals(2, decimalField.getCaretPosition());
    decimalField.setDouble(42.2);
    assertEquals("42,2", decimalField.getText());
    assertEquals(4, decimalField.getCaretPosition());
    decimalField.setText("22,3");
    assertEquals(Double.valueOf(22.3), decimalField.getDouble());
    decimalField.setText("22.3");//note this is a thousand separator
    assertEquals(Double.valueOf(223), decimalField.getDouble());
    assertEquals("223", decimalField.getText());
    decimalField.setText("22.123.123,123");
    assertEquals("22.123.123,123", decimalField.getText());
    assertEquals(Double.valueOf(22123123.123), decimalField.getDouble());
    decimalField.setText("22123123,123");
    assertEquals("22.123.123,123", decimalField.getText());
    assertEquals(Double.valueOf(22123123.123), decimalField.getDouble());

    decimalField.setSeparators('.', ',');

    decimalField.setDouble(42.2);
    assertEquals("42.2", decimalField.getText());
    decimalField.setText("22,123,123.123");
    assertEquals("22,123,123.123", decimalField.getText());
    assertEquals(Double.valueOf(22123123.123), decimalField.getDouble());
    decimalField.setText("22123123.123");
    assertEquals("22,123,123.123", decimalField.getText());
    assertEquals(Double.valueOf(22123123.123), decimalField.getDouble());

    decimalField.setDouble(10000000d);
    assertEquals("10,000,000", decimalField.getText());
    decimalField.setDouble(100000000.4d);
    assertEquals("100,000,000.4", decimalField.getText());

    decimalField.setText("2.2.2");
    assertEquals("100,000,000.4", decimalField.getText());
    decimalField.setText("..22.2.2.2");
    assertEquals("100,000,000.4", decimalField.getText());
    decimalField.setText("22.2.2.2");
    assertEquals("100,000,000.4", decimalField.getText());
    decimalField.setText("2222.2.2.2");
    assertEquals("100,000,000.4", decimalField.getText());
  }

  @Test
  public void caretPosition() throws BadLocationException {
    final DecimalField decimalField = new DecimalField();
    decimalField.setGroupingUsed(true);
    decimalField.setSeparators(',', '.');
    final NumberField.NumberDocument document = (NumberField.NumberDocument) decimalField.getDocument();

    decimalField.setText("123456789");
    assertEquals("123.456.789", decimalField.getText());

    decimalField.setCaretPosition(3);
    decimalField.moveCaretPosition(8);
    assertEquals(".456.", decimalField.getSelectedText());

    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(""), null);

    decimalField.paste();
    assertEquals("123.789", decimalField.getText());
    assertEquals(4, decimalField.getCaretPosition());

    document.insertString(3, "456", null);
    assertEquals("123.456.789", decimalField.getText());
    assertEquals(7, decimalField.getCaretPosition());

    decimalField.setCaretPosition(3);
    decimalField.moveCaretPosition(11);
    assertEquals(".456.789", decimalField.getSelectedText());

    decimalField.paste();
    assertEquals("123", decimalField.getText());
    assertEquals(3, decimalField.getCaretPosition());

    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(".456.789"), null);

    decimalField.paste();
    assertEquals("123.456.789", decimalField.getText());
    assertEquals(11, decimalField.getCaretPosition());

    decimalField.setText("");

    document.insertString(0, ",", null);
    assertEquals("0,", decimalField.getText());
    assertEquals(2, decimalField.getCaretPosition());

    decimalField.setText("");
    document.insertString(0, "1", null);
    assertEquals("1", decimalField.getText());
    assertEquals(1, decimalField.getCaretPosition());

    document.insertString(1, "2", null);
    assertEquals("12", decimalField.getText());
    assertEquals(2, decimalField.getCaretPosition());

    document.insertString(2, "3", null);
    assertEquals("123", decimalField.getText());
    assertEquals(3, decimalField.getCaretPosition());

    document.insertString(3, "4", null);
    assertEquals("1.234", decimalField.getText());
    assertEquals(5, decimalField.getCaretPosition());

    document.insertString(5, "5", null);
    assertEquals("12.345", decimalField.getText());
    assertEquals(6, decimalField.getCaretPosition());

    document.insertString(6, "6", null);
    assertEquals("123.456", decimalField.getText());
    assertEquals(7, decimalField.getCaretPosition());

    document.insertString(7, "7", null);
    assertEquals("1.234.567", decimalField.getText());
    assertEquals(9, decimalField.getCaretPosition());
  }

  @Test
  public void setMaximumFractionDigitsToZero() {
    assertThrows(IllegalArgumentException.class, () -> new DecimalField().setMaximumFractionDigits(0));
  }

  @Test
  public void setSeparatorsSameCharacter() {
    assertThrows(IllegalArgumentException.class, () -> new DecimalField().setSeparators('.', '.'));
  }

  @Test
  public void maximumFractionDigits() throws BadLocationException {
    final DecimalField decimalField = new DecimalField();
    assertEquals(-1, decimalField.getMaximumFractionDigits());
    decimalField.setSeparators(',', '.');
    decimalField.setMaximumFractionDigits(2);
    assertEquals(2, decimalField.getMaximumFractionDigits());
    decimalField.setDouble(5.1254);
    assertEquals("5,12", decimalField.getText());
    decimalField.setText("5,123");
    assertEquals("5,12", decimalField.getText());
    decimalField.getDocument().insertString(3, "4", null);
    assertEquals("5,14", decimalField.getText());
    decimalField.getDocument().remove(3, 1);
    assertEquals("5,1", decimalField.getText());
    decimalField.setMaximumFractionDigits(3);
    decimalField.setText("5,12378");
    assertEquals("5,123", decimalField.getText());//no rounding should occur
    decimalField.setMaximumFractionDigits(-1);
    decimalField.setText("5,12378");
    assertEquals("5,12378", decimalField.getText());
  }

  @Test
  public void decimalSeparators() {
    final DecimalField decimalField = new DecimalField();
    decimalField.setSeparators('.', ',');
    decimalField.setText("1.5");
    assertEquals(Double.valueOf(1.5), decimalField.getDouble());
    decimalField.setText("123.34.56");
    assertEquals(Double.valueOf(1.5), decimalField.getDouble());

    decimalField.setText("1,5");
    assertEquals(Double.valueOf(1.5), decimalField.getDouble());
    decimalField.setText("1,4.5");
    assertEquals(Double.valueOf(1.5), decimalField.getDouble());
  }

  @Test
  public void trailingDecimalSeparator() throws BadLocationException {
    final DecimalField decimalField = new DecimalField();
    decimalField.setSeparators('.', ',');
    final NumberField.NumberDocument document = (NumberField.NumberDocument) decimalField.getDocument();
    document.insertString(0, "1", null);
    assertEquals(Double.valueOf(1), decimalField.getDouble());
    document.insertString(1, ".", null);
    assertEquals("1.", decimalField.getText());
    assertEquals(Double.valueOf(1), decimalField.getDouble());
    document.insertString(2, "1", null);
    assertEquals("1.1", decimalField.getText());
    assertEquals(Double.valueOf(1.1), decimalField.getDouble());
  }

  @Test
  public void setSeparators() {
    final DecimalField decimalField = new DecimalField();
    decimalField.setGroupingUsed(true);
    decimalField.setSeparators('.', ',');
    decimalField.setNumber(12345678.9);
    assertEquals("12,345,678.9", decimalField.getText());
    decimalField.setSeparators(',', '.');
    assertEquals("12.345.678,9", decimalField.getText());
  }

  @Test
  public void trailingDecimalZeros() throws BadLocationException {
    final DecimalField decimalField = new DecimalField();
    final NumberField.NumberDocument document = (NumberField.NumberDocument) decimalField.getDocument();
    decimalField.setSeparators('.', ',');

    document.insertString(0, "1", null);
    document.insertString(1, ".", null);
    assertEquals("1.", decimalField.getText());

    document.insertString(2, "0", null);
    assertEquals("1.0", decimalField.getText());

    document.insertString(3, "0", null);
    assertEquals("1.00", decimalField.getText());

    document.insertString(4, "1", null);
    assertEquals("1.001", decimalField.getText());

    document.insertString(5, "0", null);
    assertEquals("1.0010", decimalField.getText());

    document.insertString(6, "0", null);
    assertEquals("1.00100", decimalField.getText());

    document.insertString(7, "2", null);
    assertEquals("1.001002", decimalField.getText());

    document.insertString(8, "0", null);
    assertEquals("1.0010020", decimalField.getText());

    decimalField.setText("");
    document.insertString(0, ".", null);
    assertEquals("0.", decimalField.getText());

    document.insertString(2, "0", null);
    assertEquals("0.0", decimalField.getText());

    document.insertString(3, "1", null);
    assertEquals("0.01", decimalField.getText());
  }
}
