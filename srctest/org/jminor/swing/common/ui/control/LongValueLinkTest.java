/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.jminor.common.model.Event;
import org.jminor.common.model.Events;
import org.jminor.swing.common.ui.ValueLinks;
import org.jminor.swing.common.ui.textfield.LongField;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LongValueLinkTest {

  private Long longValue;
  private final Event<Long> evtLongValueChanged = Events.event();
  private long longPrimValue;
  private final Event<Long> evtLongPrimitiveValueChanged = Events.event();

  @Test
  public void testInteger() throws Exception {
    final LongField txtLong = new LongField();
    ValueLinks.longValueLink(txtLong, this, "longValue", evtLongValueChanged, false, true);
    assertNull("Long value should be null on initialization", txtLong.getInt());
    setLongValue(2l);
    assertEquals("Long value should be 2", 2, txtLong.getLong().longValue());
    txtLong.setText("42");
    assertEquals("Long value should be 42", 42, longValue.longValue());
    txtLong.setText("");
    assertNull("Long value should be null", longValue);
  }

  @Test
  public void testInt() throws Exception {
    final LongField txtLong = new LongField();
    ValueLinks.longValueLink(txtLong, this, "longPrimValue", evtLongPrimitiveValueChanged, true, true);
    assertEquals("Long value should be 0 on initialization", Long.valueOf(0), txtLong.getLong());
    setLongPrimValue(2);
    assertEquals("Long value should be 2", 2, txtLong.getLong().longValue());
    txtLong.setText("42");
    assertEquals("Long value should be 42", 42, longPrimValue);
    txtLong.setText("");
    assertEquals("Long value should be 0", 0, longPrimValue);
  }

  public Long getLongValue() {
    return longValue;
  }

  public void setLongValue(final Long longValue) {
    this.longValue = longValue;
    evtLongValueChanged.fire(this.longValue);
  }

  public long getLongPrimValue() {
    return longPrimValue;
  }

  public void setLongPrimValue(final long longPrimValue) {
    this.longPrimValue = longPrimValue;
    evtLongPrimitiveValueChanged.fire(this.longPrimValue);
  }
}
