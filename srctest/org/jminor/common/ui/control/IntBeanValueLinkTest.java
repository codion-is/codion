/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;
import org.jminor.common.model.Events;
import org.jminor.common.ui.textfield.IntField;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class IntBeanValueLinkTest {

  private Integer integerValue;
  private final Event evtIntegerValueChanged = Events.event();
  private int intValue;
  private final Event evtIntValueChanged = Events.event();

  @Test
  public void testInteger() throws Exception {
    final IntField txtInt = new IntField();
    ValueLinks.intBeanValueLink(txtInt, this, "integerValue", evtIntegerValueChanged, false);
    assertNull("Integer value should be null on initialization", txtInt.getInt());
    setIntegerValue(2);
    assertEquals("Integer value should be 2", 2, txtInt.getInt().intValue());
    txtInt.setText("42");
    assertEquals("Integer value should be 42", 42, integerValue.intValue());
    txtInt.setText("");
    assertNull("Integer value should be null", integerValue);
  }

  @Test
  public void testInt() throws Exception {
    final IntField txtInt = new IntField();
    ValueLinks.intBeanValueLink(txtInt, this, "intValue", evtIntValueChanged, true);
    assertEquals("Int value should be 0 on initialization", (Integer) 0, txtInt.getInt());
    setIntValue(2);
    assertEquals("Int value should be 2", 2, txtInt.getInt().intValue());
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
    evtIntegerValueChanged.fire();
  }

  public int getIntValue() {
    return intValue;
  }

  public void setIntValue(final int intValue) {
    this.intValue = intValue;
    evtIntValueChanged.fire();
  }
}
