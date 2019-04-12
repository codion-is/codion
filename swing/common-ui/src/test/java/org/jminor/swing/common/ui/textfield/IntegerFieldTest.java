/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.textfield;

import org.junit.jupiter.api.Test;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.DecimalFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IntegerFieldTest {

  @Test
  public void test() {
    final IntegerField integerField = new IntegerField();
    integerField.setInteger(42);
    assertEquals("42", integerField.getText());
    integerField.setText("22");
    assertEquals(Integer.valueOf(22), integerField.getInteger());

    integerField.setInteger(10000000);
    assertEquals("10000000", integerField.getText());
    integerField.setInteger(100000000);
    assertEquals("100000000", integerField.getText());

    integerField.setRange(0, 10);
    assertEquals(0, (int) integerField.getMinimumValue());
    assertEquals(10, (int) integerField.getMaximumValue());

    integerField.setInteger(100);
    assertEquals("", integerField.getText());
    integerField.setInteger(9);
    assertEquals("9", integerField.getText());
    integerField.setInteger(-1);
    assertEquals("", integerField.getText());
    integerField.setInteger(-10);
    assertEquals("", integerField.getText());

    integerField.setRange(0, Integer.MAX_VALUE);

    final DecimalFormat decimalFormat = (DecimalFormat) ((NumberField.NumberDocument) integerField.getDocument()).getFormat();
    decimalFormat.setGroupingSize(3);
    decimalFormat.setGroupingUsed(true);
    integerField.setSeparators(',', '.');
    integerField.setText("100.000.000");
    assertEquals(100000000, (int) integerField.getInteger());
    integerField.setText("10.00.000");
    assertEquals("1.000.000", integerField.getText());
    assertEquals(1000000, (int) integerField.getInteger());
    integerField.setInteger(123456789);
    assertEquals("123.456.789", integerField.getText());
    integerField.setText("987654321");
    assertEquals(987654321, (int) integerField.getInteger());
  }

  @Test
  public void skipGroupingSeparator() {
    final IntegerField integerField = new IntegerField();
    integerField.setSeparators(',', '.');
    integerField.setGroupingUsed(true);
    final KeyListener keyListener = integerField.getKeyListeners()[0];
    integerField.setNumber(123456);
    assertEquals("123.456", integerField.getText());
    integerField.setCaretPosition(3);
    keyListener.keyReleased(new KeyEvent(integerField, KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, KeyEvent.VK_DELETE));
    assertEquals(4, integerField.getCaretPosition());
    keyListener.keyReleased(new KeyEvent(integerField, KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, KeyEvent.VK_BACK_SPACE));
    assertEquals(3, integerField.getCaretPosition());
  }
}
