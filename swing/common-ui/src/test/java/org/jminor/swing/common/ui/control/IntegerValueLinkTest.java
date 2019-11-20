/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.jminor.common.event.Event;
import org.jminor.common.event.Events;
import org.jminor.common.value.Value;
import org.jminor.common.value.Values;
import org.jminor.swing.common.ui.ValueLinks;
import org.jminor.swing.common.ui.textfield.IntegerField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class IntegerValueLinkTest {

  private Integer integerValue;
  private final Event<Integer> integerValueChangedEvent = Events.event();
  private int intValue;
  private final Event<Integer> intValueChangedEvent = Events.event();

  @Test
  public void testInteger() throws Exception {
    final IntegerField integerField = new IntegerField();
    final Value<Integer> integerPropertyValue = Values.propertyValue(this, "integerValue",
            Integer.class, integerValueChangedEvent);
    ValueLinks.integerValueLink(integerField, integerPropertyValue, true);
    assertNull(integerField.getInteger());
    setIntegerValue(2);
    assertEquals(2, integerField.getInteger().intValue());
    integerField.setText("42");
    assertEquals(42, this.integerValue.intValue());
    integerField.setText("");
    assertNull(this.integerValue);
  }

  @Test
  public void testInt() throws Exception {
    final IntegerField integerField = new IntegerField();
        final Value<Integer> integerPropertyValue = Values.propertyValue(this, "intValue",
            int.class, intValueChangedEvent);
    ValueLinks.integerValueLink(integerField, integerPropertyValue, false);
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
    integerValueChangedEvent.fire(this.integerValue);
  }

  public int getIntValue() {
    return intValue;
  }

  public void setIntValue(final int intValue) {
    this.intValue = intValue;
    intValueChangedEvent.fire(this.intValue);
  }
}
