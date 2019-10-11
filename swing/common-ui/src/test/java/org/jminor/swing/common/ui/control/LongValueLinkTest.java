/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
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
  private final Event<Long> longValueChangedEvent = Events.event();
  private long longPrimValue;
  private final Event<Long> longPrimitiveValueChangedEvent = Events.event();

  @Test
  public void testInteger() throws Exception {
    final LongField longField = new LongField();
    ValueLinks.longValueLink(longField, this, "longValue", longValueChangedEvent, false, true);
    assertNull(longField.getLong());
    setLongValue(2L);
    assertEquals(2, longField.getLong().longValue());
    longField.setText("42");
    assertEquals(42, longValue.longValue());
    longField.setText("");
    assertNull(longValue);
  }

  @Test
  public void testInt() throws Exception {
    final LongField longField = new LongField();
    ValueLinks.longValueLink(longField, this, "longPrimValue", longPrimitiveValueChangedEvent, true, true);
    assertEquals(Long.valueOf(0), longField.getLong());
    setLongPrimValue(2);
    assertEquals(2, longField.getLong().longValue());
    longField.setText("42");
    assertEquals(42, longPrimValue);
    longField.setText("");
    assertEquals(0, longPrimValue);
  }

  public Long getLongValue() {
    return longValue;
  }

  public void setLongValue(final Long longValue) {
    this.longValue = longValue;
    longValueChangedEvent.fire(this.longValue);
  }

  public long getLongPrimValue() {
    return longPrimValue;
  }

  public void setLongPrimValue(final long longPrimValue) {
    this.longPrimValue = longPrimValue;
    longPrimitiveValueChangedEvent.fire(this.longPrimValue);
  }
}
