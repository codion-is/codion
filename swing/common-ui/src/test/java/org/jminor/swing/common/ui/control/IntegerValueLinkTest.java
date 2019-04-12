/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.jminor.common.Event;
import org.jminor.common.Events;
import org.jminor.swing.common.ui.ValueLinks;
import org.jminor.swing.common.ui.textfield.IntegerField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class IntegerValueLinkTest {

  private Integer integerValue;
  private final Event<Integer> evtIntegerValueChanged = Events.event();
  private int intValue;
  private final Event<Integer> evtIntValueChanged = Events.event();

  @Test
  public void testInteger() throws Exception {
    final IntegerField integerField = new IntegerField();
    ValueLinks.integerValueLink(integerField, this, "integerValue", evtIntegerValueChanged, false, true);
    assertNull(integerField.getInteger());
    setIntegerValue(2);
    assertEquals(2, integerField.getInteger().intValue());
    integerField.setText("42");
    assertEquals(42, integerValue.intValue());
    integerField.setText("");
    assertNull(integerValue);
  }

  @Test
  public void testInt() throws Exception {
    final IntegerField integerField = new IntegerField();
    ValueLinks.integerValueLink(integerField, this, "intValue", evtIntValueChanged, true, true);
    assertEquals((Integer) 0, integerField.getInteger());
    setIntValue(2);
    assertEquals(2, integerField.getInteger().intValue());
    integerField.setText("42");
    assertEquals(42, intValue);
    integerField.setText("");
    assertEquals(0, intValue);
  }

  public Integer getIntegerValue() {
    return integerValue;
  }

  public void setIntegerValue(final Integer integerValue) {
    this.integerValue = integerValue;
    evtIntegerValueChanged.fire(this.integerValue);
  }

  public int getIntValue() {
    return intValue;
  }

  public void setIntValue(final int intValue) {
    this.intValue = intValue;
    evtIntValueChanged.fire(this.intValue);
  }
}
