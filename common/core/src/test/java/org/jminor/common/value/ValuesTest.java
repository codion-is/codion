/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.value;

import org.jminor.common.event.Event;
import org.jminor.common.event.Events;
import org.jminor.common.state.State;
import org.jminor.common.state.StateObserver;
import org.jminor.common.state.States;

import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ValuesTest {

  private final Event<Integer> integerValueChange = Events.event();
  private Integer integerValue = 42;

  public Integer getIntegerValue() {
    return integerValue;
  }

  public void setIntegerValue(final Integer integerValue) {
    if (!Objects.equals(this.integerValue, integerValue)) {
      this.integerValue = integerValue;
      integerValueChange.fire(integerValue);
    }
  }

  public Integer getIntValue() {
    return integerValue;
  }

  @Test
  public void value() {
    final AtomicInteger eventCounter = new AtomicInteger();
    final Value<Integer> intValue = Values.value(42, -1);
    assertFalse(intValue.isNullable());
    intValue.addListener(eventCounter::incrementAndGet);
    intValue.getObserver().addDataListener(data -> {
      if (eventCounter.get() != 2) {
        assertNotNull(data);
      }
    });
    intValue.set(42);
    assertEquals(0, eventCounter.get());
    intValue.set(20);
    assertEquals(1, eventCounter.get());
    intValue.set(null);
    assertEquals(-1, intValue.get());
    assertEquals(2, eventCounter.get());
    intValue.set(null);
    assertEquals(-1, intValue.get());
    assertEquals(2, eventCounter.get());
    intValue.set(42);
    assertEquals(3, eventCounter.get());
    intValue.set(null);
    assertEquals(-1, intValue.get());

    final Value<String> stringValue = Values.value(null, "null");
    assertFalse(stringValue.isNullable());
    assertEquals("null", stringValue.get());
    stringValue.set("test");
    assertEquals("test", stringValue.get());
    stringValue.set(null);
    assertEquals("null", stringValue.get());
  }

  @Test
  public void linkValues() {
    final AtomicInteger modelValueEventCounter = new AtomicInteger();
    final Value<Integer> modelValue = Values.propertyValue(this, "integerValue", Integer.class, integerValueChange.getObserver());
    final Value<Integer> uiValue = Values.value();
    Values.link(modelValue, uiValue);
    modelValue.addListener(modelValueEventCounter::incrementAndGet);
    final AtomicInteger uiValueEventCounter = new AtomicInteger();
    uiValue.addListener(uiValueEventCounter::incrementAndGet);
    assertEquals(Integer.valueOf(42), uiValue.get());
    assertEquals(0, modelValueEventCounter.get());
    assertEquals(0, uiValueEventCounter.get());

    uiValue.set(20);
    assertEquals(Integer.valueOf(20), modelValue.get());
    assertEquals(1, modelValueEventCounter.get());
    assertEquals(1, uiValueEventCounter.get());

    modelValue.set(22);
    assertEquals(Integer.valueOf(22), uiValue.get());
    assertEquals(2, modelValueEventCounter.get());
    assertEquals(2, uiValueEventCounter.get());

    uiValue.set(22);
    assertEquals(2, modelValueEventCounter.get());
    assertEquals(2, uiValueEventCounter.get());

    uiValue.set(null);
    assertNull(modelValue.get());
    assertNull(uiValue.get());
    assertEquals(3, modelValueEventCounter.get());
    assertEquals(3, uiValueEventCounter.get());
  }

  @Test
  public void linkValuesReadOnly() {
    final AtomicInteger modelValueEventCounter = new AtomicInteger();
    final Value<Integer> modelValue = Values.propertyValue(this, "intValue", Integer.class, integerValueChange.getObserver());
    final Value<Integer> uiValue = Values.value();
    Values.link(modelValue, uiValue, true);
    modelValue.addListener(modelValueEventCounter::incrementAndGet);
    final AtomicInteger uiValueEventCounter = new AtomicInteger();
    uiValue.addListener(uiValueEventCounter::incrementAndGet);
    assertEquals(Integer.valueOf(42), uiValue.get());
    assertEquals(0, modelValueEventCounter.get());
    assertEquals(0, uiValueEventCounter.get());

    uiValue.set(20);
    assertEquals(Integer.valueOf(42), modelValue.get());//read only, no change
    assertEquals(0, modelValueEventCounter.get());
    assertEquals(1, uiValueEventCounter.get());

    setIntegerValue(22);
    assertEquals(Integer.valueOf(22), uiValue.get());
    assertEquals(1, modelValueEventCounter.get());
    assertEquals(2, uiValueEventCounter.get());

    uiValue.set(22);
    assertEquals(1, modelValueEventCounter.get());
    assertEquals(2, uiValueEventCounter.get());

    uiValue.set(null);
    assertNotNull(modelValue.get());
    assertNull(uiValue.get());
    assertEquals(1, modelValueEventCounter.get());
    assertEquals(3, uiValueEventCounter.get());
  }

  @Test
  public void stateValue() {
    final State state = States.state(true);
    final Value<Boolean> stateValue = Values.stateValue(state);
    assertNotNull(stateValue.getChangeObserver());
    assertTrue(stateValue.get());
    stateValue.set(false);
    assertFalse(state.get());
    stateValue.set(null);
    assertFalse(state.get());
    stateValue.set(true);
    assertTrue(state.get());
    state.set(false);
    assertFalse(stateValue.get());
  }

  @Test
  public void booleanValueState() {
    final Value<Boolean> nullableBooleanValue = Values.value();
    final State nullableValueState = Values.valueState(nullableBooleanValue);
    assertFalse(nullableValueState.get());
    nullableBooleanValue.set(true);
    assertTrue(nullableValueState.get());
    nullableBooleanValue.set(null);
    assertFalse(nullableValueState.get());

    final Value<Boolean> booleanValue = Values.value(true, false);
    final State state = Values.valueState(booleanValue);
    final StateObserver reversed = state.getReversedObserver();
    assertTrue(state.get());
    assertFalse(reversed.get());
    state.set(false);
    assertFalse(booleanValue.get());
    booleanValue.set(true);
    assertTrue(state.get());
    assertFalse(reversed.get());
    booleanValue.set(null);
    assertFalse(state.get());
    assertTrue(reversed.get());
    assertFalse(booleanValue.get());
    state.set(true);
    assertTrue(booleanValue.get());
  }

  @Test
  public void propertyValueNoGetter() {
    assertThrows(IllegalArgumentException.class, () -> Values.propertyValue(this, "nonexistent", Integer.class, integerValueChange.getObserver()));
  }

  @Test
  public void propertyValueNoOwner() {
    assertThrows(NullPointerException.class, () -> Values.propertyValue(null, "integerValue", Integer.class, integerValueChange.getObserver()));
  }

  @Test
  public void propertyValueNoPropertyName() {
    assertThrows(IllegalArgumentException.class, () -> Values.propertyValue(this, null, Integer.class, integerValueChange.getObserver()));
  }

  @Test
  public void propertyValueNoValueClass() {
    assertThrows(NullPointerException.class, () -> Values.propertyValue(this, "integerValue", null, integerValueChange.getObserver()));
  }

  @Test
  public void setReadOnlyPropertyValue() {
    final Value<Integer> modelValue = Values.propertyValue(this, "intValue", Integer.class, integerValueChange.getObserver());
    assertThrows(IllegalStateException.class, () -> modelValue.set(42));
  }
}
