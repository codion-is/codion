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

public class DoubleFieldTest {

  @Test
  public void testNoGrouping() {
    final DoubleField doubleField = new DoubleField();
    doubleField.setSeparators(',', '.');
    doubleField.setText(",");
    assertEquals("0,", doubleField.getText());
    doubleField.setDouble(42.2);
    assertEquals("42,2", doubleField.getText());
    doubleField.setText("22,3");
    assertEquals(Double.valueOf(22.3), doubleField.getDouble());
    doubleField.setText("22.5");//note this is a thousand separator
    assertEquals(Double.valueOf(22.3), doubleField.getDouble());
    assertEquals("22,3", doubleField.getText());
    doubleField.setText("22.123.123,123");
    assertEquals("22,3", doubleField.getText());
    assertEquals(Double.valueOf(22.3), doubleField.getDouble());

    doubleField.setSeparators('.', ',');

    doubleField.setDouble(42.2);
    assertEquals("42.2", doubleField.getText());
    doubleField.setText("2,123,123.123");
    assertEquals("42.2", doubleField.getText());
    assertEquals(Double.valueOf(42.2), doubleField.getDouble());

    doubleField.setDouble(10000000d);
    assertEquals("10000000", doubleField.getText());
    doubleField.setDouble(100000000.4d);
    assertEquals("100000000.4", doubleField.getText());
  }

  @Test
  public void testGrouping() {
    final DoubleField doubleField = new DoubleField();
    doubleField.setGroupingUsed(true);
    doubleField.setSeparators(',', '.');
    assertEquals(0, doubleField.getCaretPosition());
    doubleField.setText(",");
    assertEquals("0,", doubleField.getText());
    assertEquals(2, doubleField.getCaretPosition());
    doubleField.setDouble(42.2);
    assertEquals("42,2", doubleField.getText());
    assertEquals(4, doubleField.getCaretPosition());
    doubleField.setText("22,3");
    assertEquals(Double.valueOf(22.3), doubleField.getDouble());
    doubleField.setText("22.3");//note this is a thousand separator
    assertEquals(Double.valueOf(223), doubleField.getDouble());
    assertEquals("223", doubleField.getText());
    doubleField.setText("22.123.123,123");
    assertEquals("22.123.123,123", doubleField.getText());
    assertEquals(Double.valueOf(22123123.123), doubleField.getDouble());
    doubleField.setText("22123123,123");
    assertEquals("22.123.123,123", doubleField.getText());
    assertEquals(Double.valueOf(22123123.123), doubleField.getDouble());

    doubleField.setSeparators('.', ',');

    doubleField.setDouble(42.2);
    assertEquals("42.2", doubleField.getText());
    doubleField.setText("22,123,123.123");
    assertEquals("22,123,123.123", doubleField.getText());
    assertEquals(Double.valueOf(22123123.123), doubleField.getDouble());
    doubleField.setText("22123123.123");
    assertEquals("22,123,123.123", doubleField.getText());
    assertEquals(Double.valueOf(22123123.123), doubleField.getDouble());

    doubleField.setDouble(10000000d);
    assertEquals("10,000,000", doubleField.getText());
    doubleField.setDouble(100000000.4d);
    assertEquals("100,000,000.4", doubleField.getText());

    doubleField.setText("2.2.2");
    assertEquals("100,000,000.4", doubleField.getText());
    doubleField.setText("..22.2.2.2");
    assertEquals("100,000,000.4", doubleField.getText());
    doubleField.setText("22.2.2.2");
    assertEquals("100,000,000.4", doubleField.getText());
    doubleField.setText("2222.2.2.2");
    assertEquals("100,000,000.4", doubleField.getText());
  }

  @Test
  public void caretPosition() throws BadLocationException {
    final DoubleField doubleField = new DoubleField();
    doubleField.setGroupingUsed(true);
    doubleField.setSeparators(',', '.');
    final NumberField.NumberDocument document = (NumberField.NumberDocument) doubleField.getDocument();

    doubleField.setText("123456789");
    assertEquals("123.456.789", doubleField.getText());

    doubleField.setCaretPosition(3);
    doubleField.moveCaretPosition(8);
    assertEquals(".456.", doubleField.getSelectedText());

    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(""), null);

    doubleField.paste();
    assertEquals("123.789", doubleField.getText());
    assertEquals(4, doubleField.getCaretPosition());

    document.insertString(3, "456", null);
    assertEquals("123.456.789", doubleField.getText());
    assertEquals(7, doubleField.getCaretPosition());

    doubleField.setCaretPosition(3);
    doubleField.moveCaretPosition(11);
    assertEquals(".456.789", doubleField.getSelectedText());

    doubleField.paste();
    assertEquals("123", doubleField.getText());
    assertEquals(3, doubleField.getCaretPosition());

    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(".456.789"), null);

    doubleField.paste();
    assertEquals("123.456.789", doubleField.getText());
    assertEquals(11, doubleField.getCaretPosition());

    doubleField.setText("");

    document.insertString(0, ",", null);
    assertEquals("0,", doubleField.getText());
    assertEquals(2, doubleField.getCaretPosition());

    doubleField.setText("");
    document.insertString(0, "1", null);
    assertEquals("1", doubleField.getText());
    assertEquals(1, doubleField.getCaretPosition());

    document.insertString(1, "2", null);
    assertEquals("12", doubleField.getText());
    assertEquals(2, doubleField.getCaretPosition());

    document.insertString(2, "3", null);
    assertEquals("123", doubleField.getText());
    assertEquals(3, doubleField.getCaretPosition());

    document.insertString(3, "4", null);
    assertEquals("1.234", doubleField.getText());
    assertEquals(5, doubleField.getCaretPosition());

    document.insertString(5, "5", null);
    assertEquals("12.345", doubleField.getText());
    assertEquals(6, doubleField.getCaretPosition());

    document.insertString(6, "6", null);
    assertEquals("123.456", doubleField.getText());
    assertEquals(7, doubleField.getCaretPosition());

    document.insertString(7, "7", null);
    assertEquals("1.234.567", doubleField.getText());
    assertEquals(9, doubleField.getCaretPosition());
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
    final DoubleField doubleField = new DoubleField();
    assertEquals(-1, doubleField.getMaximumFractionDigits());
    doubleField.setSeparators(',', '.');
    doubleField.setMaximumFractionDigits(2);
    assertEquals(2, doubleField.getMaximumFractionDigits());
    doubleField.setDouble(5.1254);
    assertEquals("5,12", doubleField.getText());
    doubleField.setText("5,123");
    assertEquals("5,12", doubleField.getText());
    doubleField.getDocument().insertString(3, "4", null);
    assertEquals("5,14", doubleField.getText());
    doubleField.getDocument().remove(3, 1);
    assertEquals("5,1", doubleField.getText());
    doubleField.setMaximumFractionDigits(3);
    doubleField.setText("5,12378");
    assertEquals("5,123", doubleField.getText());//no rounding should occur
    doubleField.setMaximumFractionDigits(-1);
    doubleField.setText("5,12378");
    assertEquals("5,12378", doubleField.getText());
  }

  @Test
  public void decimalSeparators() {
    final DoubleField doubleField = new DoubleField();
    doubleField.setSeparators('.', ',');
    doubleField.setText("1.5");
    assertEquals(Double.valueOf(1.5), doubleField.getDouble());
    doubleField.setText("123.34.56");
    assertEquals(Double.valueOf(1.5), doubleField.getDouble());

    doubleField.setText("1,5");
    assertEquals(Double.valueOf(1.5), doubleField.getDouble());
    doubleField.setText("1,4.5");
    assertEquals(Double.valueOf(1.5), doubleField.getDouble());
  }

  @Test
  public void trailingDecimalSeparator() throws BadLocationException {
    final DoubleField doubleField = new DoubleField();
    doubleField.setSeparators('.', ',');
    final NumberField.NumberDocument document = (NumberField.NumberDocument) doubleField.getDocument();
    document.insertString(0, "1", null);
    assertEquals(Double.valueOf(1), doubleField.getDouble());
    document.insertString(1, ".", null);
    assertEquals("1.", doubleField.getText());
    assertEquals(Double.valueOf(1), doubleField.getDouble());
    document.insertString(2, "1", null);
    assertEquals("1.1", doubleField.getText());
    assertEquals(Double.valueOf(1.1), doubleField.getDouble());
  }

  @Test
  public void setSeparators() {
    final DoubleField doubleField = new DoubleField();
    doubleField.setGroupingUsed(true);
    doubleField.setSeparators('.', ',');
    doubleField.setNumber(12345678.9);
    assertEquals("12,345,678.9", doubleField.getText());
    doubleField.setSeparators(',', '.');
    assertEquals("12.345.678,9", doubleField.getText());
  }

  @Test
  public void trailingDecimalZeros() throws BadLocationException {
    final DoubleField doubleField = new DoubleField();
    final NumberField.NumberDocument document = (NumberField.NumberDocument) doubleField.getDocument();
    doubleField.setSeparators('.', ',');

    document.insertString(0, "1", null);
    document.insertString(1, ".", null);
    assertEquals("1.", doubleField.getText());

    document.insertString(2, "0", null);
    assertEquals("1.0", doubleField.getText());

    document.insertString(3, "0", null);
    assertEquals("1.00", doubleField.getText());

    document.insertString(4, "1", null);
    assertEquals("1.001", doubleField.getText());

    document.insertString(5, "0", null);
    assertEquals("1.0010", doubleField.getText());

    document.insertString(6, "0", null);
    assertEquals("1.00100", doubleField.getText());

    document.insertString(7, "2", null);
    assertEquals("1.001002", doubleField.getText());

    document.insertString(8, "0", null);
    assertEquals("1.0010020", doubleField.getText());

    doubleField.setText("");
    document.insertString(0, ".", null);
    assertEquals("0.", doubleField.getText());

    document.insertString(2, "0", null);
    assertEquals("0.0", doubleField.getText());

    document.insertString(3, "1", null);
    assertEquals("0.01", doubleField.getText());
  }
}
