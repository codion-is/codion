/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

import is.codion.common.event.Event;
import is.codion.common.event.Events;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.state.States;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
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
      integerValueChange.onEvent(integerValue);
    }
  }

  public int getIntValue() {
    return integerValue;
  }

  @Test
  public void value() {
    final AtomicInteger eventCounter = new AtomicInteger();
    final AtomicInteger observerCounter = new AtomicInteger();
    final Value<Integer> intValue = Values.value(42, -1);
    assertFalse(intValue.isNullable());
    final ValueObserver<Integer> valueObserver = Values.valueObserver(intValue);
    intValue.addListener(eventCounter::incrementAndGet);
    intValue.addDataListener(data -> {
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
    assertEquals(-1, valueObserver.get());
    assertEquals(2, eventCounter.get());
    intValue.set(null);
    assertEquals(-1, intValue.get());
    assertEquals(-1, valueObserver.get());
    assertEquals(2, eventCounter.get());
    intValue.set(42);
    assertEquals(3, eventCounter.get());
    intValue.set(null);
    assertEquals(-1, intValue.get());
    assertEquals(-1, valueObserver.get());

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
    modelValue.link(uiValue);
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

    final Value<Integer> valueOne = Values.value();
    final Value<Integer> valueTwo = Values.value();
    valueOne.link(valueTwo);
    valueTwo.link(valueOne);
    valueOne.set(1);
    assertThrows(IllegalArgumentException.class, () -> valueOne.link(valueOne));
  }

  @Test
  public void linkValuesReadOnly() {
    final AtomicInteger modelValueEventCounter = new AtomicInteger();
    final Value<Integer> modelValue = Values.propertyValue(this, "intValue", int.class, integerValueChange.getObserver());
    final Value<Integer> uiValue = Values.value();
    assertFalse(modelValue.isNullable());
    modelValue.addDataListener(uiValue::set);//modelValue.link(uiValue, true);
    uiValue.set(modelValue.get());
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
  public void getOrThrow() {
    final PropertyValue<Integer> integerValue = Values.propertyValue(this, "integerValue", Integer.class, integerValueChange.getObserver());
    integerValue.set(null);
    assertThrows(IllegalStateException.class, integerValue::getOrThrow);
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

  @Test
  public void valueSet() {
    ValueSet<Integer> valueSet = Values.valueSet();

    assertFalse(valueSet.isNullable());

    assertTrue(valueSet.add(1));
    assertFalse(valueSet.add(1));
    assertTrue(valueSet.remove(1));
    assertFalse(valueSet.remove(1));

    final Set<Integer> initialValues = new HashSet<>();
    initialValues.add(1);
    initialValues.add(2);

    valueSet = Values.valueSet(initialValues);
    assertEquals(initialValues, valueSet.get());

    assertFalse(valueSet.add(1));
    assertFalse(valueSet.add(2));
    assertTrue(valueSet.add(3));
    assertTrue(valueSet.remove(1));
    assertTrue(valueSet.remove(2));

    valueSet.set(initialValues);
    assertTrue(valueSet.remove(1));
    assertTrue(valueSet.remove(2));
    assertTrue(valueSet.add(3));

    valueSet.clear();
    assertTrue(valueSet.add(3));
    assertFalse(valueSet.remove(1));
    assertFalse(valueSet.remove(2));

    assertTrue(valueSet.add(null));
    assertFalse(valueSet.add(null));
    assertTrue(valueSet.remove(null));

    valueSet.clear();
    valueSet.add(1);
    valueSet.add(2);

    valueSet.set(null);
    assertTrue(valueSet.add(1));
    assertTrue(valueSet.add(2));

    valueSet.clear();

    final Value<Integer> value = valueSet.value();

    value.set(1);
    assertTrue(valueSet.get().contains(1));
    value.set(null);
    assertTrue(valueSet.get().isEmpty());

    valueSet.set(Collections.singleton(2));
    assertEquals(2, value.get());

    valueSet.clear();
    assertNull(value.get());
  }
}
