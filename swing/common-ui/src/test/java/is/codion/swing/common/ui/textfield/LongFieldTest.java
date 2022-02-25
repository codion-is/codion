/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LongFieldTest {

  @Test
  void test() {
    LongField longField = new LongField(5);
    longField.setLong(42L);
    assertEquals("42", longField.getText());
    longField.setText("22");
    assertEquals(Long.valueOf(22), longField.getLong());

    longField.setLong(10000000000000L);
    assertEquals("10000000000000", longField.getText());
    longField.setLong(1000000000000L);
    assertEquals("1000000000000", longField.getText());

    longField.setRange(0, 10);
    assertEquals(0, (int) longField.getMinimumValue());
    assertEquals(10, (int) longField.getMaximumValue());

    longField.setText("");
    assertThrows(IllegalArgumentException.class, () -> longField.setLong(100L));
    assertEquals("", longField.getText());
    longField.setLong(9L);
    assertEquals("9", longField.getText());
    longField.setText("");
    assertThrows(IllegalArgumentException.class, () -> longField.setLong(-1L));
    assertEquals("", longField.getText());
  }
}
