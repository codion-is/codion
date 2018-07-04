/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.jminor.common.Event;
import org.jminor.common.Events;
import org.jminor.swing.common.ui.ValueLinks;
import org.jminor.swing.common.ui.textfield.LongField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class LongValueLinkTest {

  private Long longValue;
  private final Event<Long> evtLongValueChanged = Events.event();
  private long longPrimValue;
  private final Event<Long> evtLongPrimitiveValueChanged = Events.event();

  @Test
  public void testInteger() throws Exception {
    final LongField txtLong = new LongField();
    ValueLinks.longValueLink(txtLong, this, "longValue", evtLongValueChanged, false, true);
    assertNull(txtLong.getLong());
    setLongValue(2L);
    assertEquals(2, txtLong.getLong().longValue());
    txtLong.setText("42");
    assertEquals(42, longValue.longValue());
    txtLong.setText("");
    assertNull(longValue);
  }

  @Test
  public void testInt() throws Exception {
    final LongField txtLong = new LongField();
    ValueLinks.longValueLink(txtLong, this, "longPrimValue", evtLongPrimitiveValueChanged, true, true);
    assertEquals(Long.valueOf(0), txtLong.getLong());
    setLongPrimValue(2);
    assertEquals(2, txtLong.getLong().longValue());
    txtLong.setText("42");
    assertEquals(42, longPrimValue);
    txtLong.setText("");
    assertEquals(0, longPrimValue);
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
