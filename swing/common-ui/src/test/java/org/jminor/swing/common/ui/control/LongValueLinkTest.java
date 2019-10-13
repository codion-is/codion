/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.jminor.common.Event;
import org.jminor.common.Events;
import org.jminor.common.Value;
import org.jminor.common.Values;
import org.jminor.swing.common.ui.UpdateTrigger;
import org.jminor.swing.common.ui.ValueLinks;
import org.jminor.swing.common.ui.textfield.LongField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class LongValueLinkTest {

  private Long longValue;
  private final Event<Long> longValueChangedEvent = Events.event();
  private long longPrimitiveValue;
  private final Event<Long> longPrimitiveValueChangedEvent = Events.event();

  @Test
  public void testLong() throws Exception {
    final LongField longField = new LongField();
    final Value<Long> longPropertyValue = Values.propertyValue(this, "longValue",
            Long.class, longValueChangedEvent);
    ValueLinks.longValueLink(longField, longPropertyValue, true, UpdateTrigger.KEYSTROKE);
    assertNull(longField.getLong());
    setLongValue(2L);
    assertEquals(2, longField.getLong().longValue());
    longField.setText("42");
    assertEquals(42, this.longValue.longValue());
    longField.setText("");
    assertNull(this.longValue);
  }

  @Test
  public void testLongPrimitive() throws Exception {
    final LongField longField = new LongField();
    final Value<Long> longPrimitivePropertyValue = Values.propertyValue(this, "longPrimitiveValue",
            long.class, longPrimitiveValueChangedEvent);
    ValueLinks.longValueLink(longField, longPrimitivePropertyValue, false, UpdateTrigger.KEYSTROKE);
    assertEquals(Long.valueOf(0), longField.getLong());
    setLongPrimitiveValue(2);
    assertEquals(2, longField.getLong().longValue());
    longField.setText("42");
    assertEquals(42, longPrimitiveValue);
    longField.setText("");
    assertEquals(0, longPrimitiveValue);
  }

  public Long getLongValue() {
    return longValue;
  }

  public void setLongValue(final Long longValue) {
    this.longValue = longValue;
    longValueChangedEvent.fire(this.longValue);
  }

  public long getLongPrimitiveValue() {
    return longPrimitiveValue;
  }

  public void setLongPrimitiveValue(final long longPrimitiveValue) {
    this.longPrimitiveValue = longPrimitiveValue;
    longPrimitiveValueChangedEvent.fire(this.longPrimitiveValue);
  }
}
