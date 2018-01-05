/*
 * Chinook.Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.textfield;

import org.junit.Test;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.DecimalFormat;

import static org.junit.Assert.assertEquals;

public class IntegerFieldTest {

  @Test
  public void test() {
    final IntegerField txt = new IntegerField();
    txt.setInteger(42);
    assertEquals("42", txt.getText());
    txt.setText("22");
    assertEquals(Integer.valueOf(22), txt.getInteger());

    txt.setInteger(10000000);
    assertEquals("10000000", txt.getText());
    txt.setInteger(100000000);
    assertEquals("100000000", txt.getText());

    txt.setRange(0, 10);
    assertEquals(0, (int) txt.getMinimumValue());
    assertEquals(10, (int) txt.getMaximumValue());

    txt.setInteger(100);
    assertEquals("", txt.getText());
    txt.setInteger(9);
    assertEquals("9", txt.getText());
    txt.setInteger(-1);
    assertEquals("", txt.getText());
    txt.setInteger(-10);
    assertEquals("", txt.getText());

    txt.setRange(0, Integer.MAX_VALUE);

    final DecimalFormat decimalFormat = (DecimalFormat) ((NumberField.NumberDocument) txt.getDocument()).getFormat();
    decimalFormat.setGroupingSize(3);
    decimalFormat.setGroupingUsed(true);
    txt.setSeparators(',', '.');
    txt.setText("100.000.000");
    assertEquals(100000000, (int) txt.getInteger());
    txt.setText("10.00.000");
    assertEquals("1.000.000", txt.getText());
    assertEquals(1000000, (int) txt.getInteger());
    txt.setInteger(123456789);
    assertEquals("123.456.789", txt.getText());
    txt.setText("987654321");
    assertEquals(987654321, (int) txt.getInteger());
  }

  @Test
  public void skipGroupingSeparator() {
    final IntegerField txt = new IntegerField();
    txt.setSeparators(',', '.');
    txt.setGroupingUsed(true);
    final KeyListener keyListener = txt.getKeyListeners()[0];
    txt.setNumber(123456);
    assertEquals("123.456", txt.getText());
    txt.setCaretPosition(3);
    keyListener.keyReleased(new KeyEvent(txt, KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, KeyEvent.VK_DELETE));
    assertEquals(4, txt.getCaretPosition());
    keyListener.keyReleased(new KeyEvent(txt, KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, KeyEvent.VK_BACK_SPACE));
    assertEquals(3, txt.getCaretPosition());
  }
}
