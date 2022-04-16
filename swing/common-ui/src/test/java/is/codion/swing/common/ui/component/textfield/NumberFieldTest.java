/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.textfield;

import org.junit.jupiter.api.Test;

import javax.swing.text.BadLocationException;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.DecimalFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class NumberFieldTest {

  @Test
  void integerFieldTest() {
    NumberField<Integer> integerField = NumberField.integerField();
    integerField.setGroupingUsed(false);
    integerField.setNumber(42);
    assertEquals("42", integerField.getText());
    integerField.setText("22");
    assertEquals(Integer.valueOf(22), integerField.getNumber());

    integerField.setNumber(10000000);
    assertEquals("10000000", integerField.getText());
    integerField.setNumber(100000000);
    assertEquals("100000000", integerField.getText());

    integerField.setRange(0, 10);
    assertEquals(0, (int) integerField.getMinimumValue());
    assertEquals(10, (int) integerField.getMaximumValue());

    assertThrows(IllegalArgumentException.class, () -> integerField.setNumber(100));
    assertEquals("", integerField.getText());
    integerField.setNumber(9);
    assertEquals("9", integerField.getText());
    assertThrows(IllegalArgumentException.class, () -> integerField.setNumber(-1));
    assertEquals("", integerField.getText());
    assertThrows(IllegalArgumentException.class, () -> integerField.setNumber(-10));
    assertEquals("", integerField.getText());

    assertThrows(IllegalStateException.class, integerField::getMaximumFractionDigits);
    assertThrows(IllegalStateException.class, () -> integerField.setMaximumFractionDigits(2));

    integerField.setRange(0, Integer.MAX_VALUE);

    DecimalFormat decimalFormat = (DecimalFormat) ((NumberDocument<Integer>) integerField.getDocument()).getFormat();
    decimalFormat.setGroupingSize(3);
    decimalFormat.setGroupingUsed(true);
    integerField.setSeparators(',', '.');
    integerField.setText("100.000.000");
    assertEquals(100000000, (int) integerField.getNumber());
    integerField.setText("10.00.000");
    assertEquals("1.000.000", integerField.getText());
    assertEquals(1000000, (int) integerField.getNumber());
    integerField.setNumber(123456789);
    assertEquals("123.456.789", integerField.getText());
    integerField.setText("987654321");
    assertEquals(987654321, (int) integerField.getNumber());

    integerField.setNumber(null);
    integerField.addValueListener(value -> assertEquals(42, value));
    integerField.setNumber(42);
  }

  @Test
  void skipGroupingSeparator() {
    NumberField<Integer> integerField = NumberField.integerField();
    integerField.setSeparators(',', '.');
    integerField.setGroupingUsed(true);
    KeyListener keyListener = integerField.getKeyListeners()[0];
    integerField.setNumber(123456);
    assertEquals("123.456", integerField.getText());
    integerField.setCaretPosition(3);
    keyListener.keyReleased(new KeyEvent(integerField, KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0,
            KeyEvent.VK_DELETE, KeyEvent.CHAR_UNDEFINED));
    assertEquals(4, integerField.getCaretPosition());
    keyListener.keyReleased(new KeyEvent(integerField, KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0,
            KeyEvent.VK_BACK_SPACE, KeyEvent.CHAR_UNDEFINED));
    assertEquals(3, integerField.getCaretPosition());
  }

  @Test
  void longFieldTest() {
    NumberField<Long> longField = NumberField.longField();
    longField.setGroupingUsed(false);
    longField.setNumber(42L);
    assertEquals("42", longField.getText());
    longField.setText("22");
    assertEquals(Long.valueOf(22), longField.getNumber());

    longField.setNumber(10000000000000L);
    assertEquals("10000000000000", longField.getText());
    longField.setNumber(1000000000000L);
    assertEquals("1000000000000", longField.getText());

    longField.setRange(0, 10);
    assertEquals(0, (int) longField.getMinimumValue());
    assertEquals(10, (int) longField.getMaximumValue());

    longField.setText("");
    assertThrows(IllegalArgumentException.class, () -> longField.setNumber(100L));
    assertEquals("", longField.getText());
    longField.setNumber(9L);
    assertEquals("9", longField.getText());
    longField.setText("");
    assertThrows(IllegalArgumentException.class, () -> longField.setNumber(-1L));
    assertEquals("", longField.getText());
  }

  @Test
  void testNoGrouping() {
    NumberField<Double>doubleField = NumberField.doubleField();
    doubleField.setGroupingUsed(false);
    doubleField.setSeparators(',', '.');
    doubleField.setText(",");
    assertEquals("0,", doubleField.getText());
    doubleField.setNumber(42.2);
    assertEquals("42,2", doubleField.getText());
    doubleField.setText("22,3");
    assertEquals(Double.valueOf(22.3), doubleField.getNumber());
    doubleField.setText("22.5");//note this is a thousand separator
    assertEquals(Double.valueOf(22.3), doubleField.getNumber());
    assertEquals("22,3", doubleField.getText());
    doubleField.setText("22.123.123,123");
    assertEquals("22,3", doubleField.getText());
    assertEquals(Double.valueOf(22.3), doubleField.getNumber());

    doubleField.setSeparators('.', ',');

    doubleField.setNumber(42.2);
    assertEquals("42.2", doubleField.getText());
    doubleField.setText("2,123,123.123");
    assertEquals("42.2", doubleField.getText());
    assertEquals(Double.valueOf(42.2), doubleField.getNumber());

    doubleField.setNumber(10000000d);
    assertEquals("10000000", doubleField.getText());
    doubleField.setNumber(100000000.4d);
    assertEquals("100000000.4", doubleField.getText());
  }

  @Test
  void testGrouping() {
    DecimalFormat decimalFormat = new DecimalFormat();
    NumberField<Double>doubleField = NumberField.doubleField(decimalFormat);
    doubleField.setGroupingUsed(true);
    doubleField.setSeparators(',', '.');
    assertEquals(0, doubleField.getCaretPosition());
    doubleField.setText(",");
    assertEquals("0,", doubleField.getText());
    assertEquals(2, doubleField.getCaretPosition());
    doubleField.setNumber(42.2);
    assertEquals("42,2", doubleField.getText());
    assertEquals(4, doubleField.getCaretPosition());
    doubleField.setText("22,3");
    assertEquals(Double.valueOf(22.3), doubleField.getNumber());
    doubleField.setText("22.3");//note this is a thousand separator
    assertEquals(Double.valueOf(223), doubleField.getNumber());
    assertEquals("223", doubleField.getText());
    doubleField.setText("22.123.123,123");
    assertEquals("22.123.123,123", doubleField.getText());
    assertEquals(Double.valueOf(22123123.123), doubleField.getNumber());
    doubleField.setText("22123123,123");
    assertEquals("22.123.123,123", doubleField.getText());
    assertEquals(Double.valueOf(22123123.123), doubleField.getNumber());

    doubleField.setSeparators('.', ',');

    doubleField.setNumber(42.2);
    assertEquals("42.2", doubleField.getText());
    doubleField.setText("22,123,123.123");
    assertEquals("22,123,123.123", doubleField.getText());
    assertEquals(Double.valueOf(22123123.123), doubleField.getNumber());
    doubleField.setText("22123123.123");
    assertEquals("22,123,123.123", doubleField.getText());
    assertEquals(Double.valueOf(22123123.123), doubleField.getNumber());

    doubleField.setNumber(10000000d);
    assertEquals("10,000,000", doubleField.getText());
    doubleField.setNumber(100000000.4d);
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
  void caretPosition() throws BadLocationException {
    NumberField<Double>doubleField = NumberField.doubleField();
    doubleField.setGroupingUsed(true);
    doubleField.setSeparators(',', '.');
    NumberDocument<Double> document = doubleField.getTypedDocument();

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
  void setSeparatorsSameCharacter() {
    assertThrows(IllegalArgumentException.class, () -> NumberField.doubleField().setSeparators('.', '.'));
  }

  @Test
  void maximumFractionDigits() throws BadLocationException {
    NumberField<Double>doubleField = NumberField.doubleField();
    assertEquals(-1, doubleField.getMaximumFractionDigits());
    doubleField.setSeparators(',', '.');
    doubleField.setMaximumFractionDigits(2);
    assertEquals(2, doubleField.getMaximumFractionDigits());
    doubleField.setNumber(5.1254);
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
  void decimalSeparators() {
    NumberField<Double>doubleField = NumberField.doubleField();
    doubleField.setGroupingUsed(false);
    doubleField.setSeparators('.', ',');
    doubleField.setText("1.5");
    assertEquals(Double.valueOf(1.5), doubleField.getNumber());
    doubleField.setText("123.34.56");
    assertEquals(Double.valueOf(1.5), doubleField.getNumber());

    doubleField.setText("1,5");
    assertEquals(Double.valueOf(1.5), doubleField.getNumber());
    doubleField.setText("1,4.5");
    assertEquals(Double.valueOf(1.5), doubleField.getNumber());
  }

  @Test
  void trailingDecimalSeparator() throws BadLocationException {
    NumberField<Double>doubleField = NumberField.doubleField();
    doubleField.setSeparators('.', ',');
    NumberDocument<Double> document = doubleField.getTypedDocument();
    document.insertString(0, "1", null);
    assertEquals(Double.valueOf(1), doubleField.getNumber());
    document.insertString(1, ".", null);
    assertEquals("1.", doubleField.getText());
    assertEquals(Double.valueOf(1), doubleField.getNumber());
    document.insertString(2, "1", null);
    assertEquals("1.1", doubleField.getText());
    assertEquals(Double.valueOf(1.1), doubleField.getNumber());
  }

  @Test
  void setSeparators() {
    NumberField<Double>doubleField = NumberField.doubleField();
    doubleField.setGroupingUsed(true);
    doubleField.setSeparators('.', ',');
    doubleField.setNumber(12345678.9);
    assertEquals("12,345,678.9", doubleField.getText());
    doubleField.setSeparators(',', '.');
    assertEquals("12.345.678,9", doubleField.getText());
  }

  @Test
  void trailingDecimalZeros() throws BadLocationException {
    NumberField<Double>doubleField = NumberField.doubleField();
    NumberDocument<Double> document = doubleField.getTypedDocument();
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
