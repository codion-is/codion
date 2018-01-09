/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.jminor.common.Event;
import org.jminor.common.Events;
import org.jminor.swing.common.ui.ValueLinks;
import org.jminor.swing.common.ui.textfield.IntegerField;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class IntegerValueLinkTest {

  private Integer integerValue;
  private final Event<Integer> evtIntegerValueChanged = Events.event();
  private int intValue;
  private final Event<Integer> evtIntValueChanged = Events.event();

  @Test
  public void testInteger() throws Exception {
    final IntegerField txtInt = new IntegerField();
    ValueLinks.integerValueLink(txtInt, this, "integerValue", evtIntegerValueChanged, false, true);
    assertNull("Integer value should be null on initialization", txtInt.getInteger());
    setIntegerValue(2);
    assertEquals("Integer value should be 2", 2, txtInt.getInteger().intValue());
    txtInt.setText("42");
    assertEquals("Integer value should be 42", 42, integerValue.intValue());
    txtInt.setText("");
    assertNull("Integer value should be null", integerValue);
  }

  @Test
  public void testInt() throws Exception {
    final IntegerField txtInt = new IntegerField();
    ValueLinks.integerValueLink(txtInt, this, "intValue", evtIntValueChanged, true, true);
    assertEquals("Int value should be 0 on initialization", (Integer) 0, txtInt.getInteger());
    setIntValue(2);
    assertEquals("Int value should be 2", 2, txtInt.getInteger().intValue());
    txtInt.setText("42");
    assertEquals("Int value should be 42", 42, intValue);
    txtInt.setText("");
    assertEquals("Int value should be 0", 0, intValue);
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
