/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ValuesTest {

  private final Event<Integer> integerValueChange = Event.event();
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
  public void validator() {
    final Value.Validator<Integer> validator = value -> {
      if (value != null && value > 10) {
        throw new IllegalArgumentException();
      }
    };
    final Value<Integer> value = Values.value(0, 0);
    value.set(11);
    assertThrows(IllegalArgumentException.class, () -> value.addValidator(validator));
    value.set(1);
    value.addValidator(validator);
    value.addValidator(validator);
    assertEquals(1, value.getValidators().size());
    value.set(null);
    assertEquals(0, value.get());
    assertThrows(IllegalArgumentException.class, () -> value.set(11));
    value.set(2);
    assertEquals(2, value.get());
    assertThrows(IllegalArgumentException.class, () -> value.set(12));
  }

  @Test
  public void value() {
    final AtomicInteger eventCounter = new AtomicInteger();
    final Value<Integer> intValue = Values.value(42, -1);
    assertFalse(intValue.isNullable());
    assertTrue(intValue.toOptional().isPresent());
    assertTrue(intValue.is(42));
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
    assertTrue(intValue.is(20));
    intValue.set(null);
    assertTrue(intValue.is(-1));
    assertFalse(intValue.isNull());
    assertTrue(intValue.isNotNull());
    assertTrue(intValue.toOptional().isPresent());
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

    final Value<String> value = Values.value();
    assertFalse(value.toOptional().isPresent());
    value.set("hello");
    assertTrue(value.toOptional().isPresent());
  }

  @Test
  public void linkValues() {
    final AtomicInteger modelValueEventCounter = new AtomicInteger();
    final Value<Integer> modelValue = Values.propertyValue(this, "integerValue", Integer.class, integerValueChange.getObserver());
    final Value<Integer> uiValue = Values.value();
    uiValue.link(modelValue);
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
    assertTrue(modelValue.isNull());
    assertFalse(modelValue.isNotNull());
    assertTrue(modelValue.is(null));
    assertNull(uiValue.get());
    assertTrue(uiValue.isNull());
    assertEquals(3, modelValueEventCounter.get());
    assertEquals(3, uiValueEventCounter.get());

    final Value<Integer> valueOne = Values.value();
    final Value<Integer> valueTwo = Values.value();
    valueTwo.link(valueOne);
    valueOne.link(valueTwo);
    valueOne.set(1);
    assertThrows(IllegalArgumentException.class, () -> valueOne.link(valueOne));
  }

  @Test
  public void linkValuesReadOnly() {
    final AtomicInteger modelValueEventCounter = new AtomicInteger();
    final Value<Integer> modelValue = Values.propertyValue(this, "intValue", int.class, integerValueChange.getObserver());
    final Value<Integer> uiValue = Values.value();
    assertFalse(modelValue.isNullable());
    uiValue.link(Values.valueObserver(modelValue));
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
    final State state = State.state(true);
    final Value<Boolean> stateValue = Values.stateValue(state);
    assertTrue(stateValue.toOptional().isPresent());
    assertTrue(stateValue.get());
    stateValue.set(false);
    assertFalse(state.get());
    stateValue.set(null);
    assertFalse(stateValue.isNull());
    assertTrue(stateValue.isNotNull());
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
    assertTrue(valueSet.isEmpty());
    assertFalse(valueSet.isNotEmpty());

    assertFalse(valueSet.isNullable());
    assertTrue(valueSet.toOptional().isPresent());

    assertTrue(valueSet.add(1));
    assertFalse(valueSet.add(1));
    assertTrue(valueSet.remove(1));
    assertFalse(valueSet.remove(1));

    final Set<Integer> initialValues = new HashSet<>();
    initialValues.add(1);
    initialValues.add(2);

    valueSet = Values.valueSet(initialValues);
    assertFalse(valueSet.isEmpty());
    assertTrue(valueSet.isNotEmpty());
    assertEquals(initialValues, valueSet.get());
    assertTrue(valueSet.is(initialValues));

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
    assertTrue(valueSet.isEmpty());
    assertFalse(valueSet.isNotEmpty());
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

  @Test
  public void valueSetEvents() {
    final ValueSet<Integer> valueSet = Values.valueSet();
    final Value<Integer> value = valueSet.value();

    final AtomicInteger valueEventCounter = new AtomicInteger();
    final EventDataListener<Integer> listener = integer -> valueEventCounter.incrementAndGet();
    value.addDataListener(listener);

    final AtomicInteger valueSetEventCounter = new AtomicInteger();
    final EventDataListener<Set<Integer>> setListener = integers -> valueSetEventCounter.incrementAndGet();
    valueSet.addDataListener(setListener);

    valueSet.add(1);

    assertEquals(1, valueEventCounter.get());
    assertEquals(1, valueSetEventCounter.get());

    value.set(2);

    assertEquals(2, valueEventCounter.get());
    assertEquals(2, valueSetEventCounter.get());

    valueSet.clear();

    assertEquals(3, valueEventCounter.get());
    assertEquals(3, valueSetEventCounter.get());
  }

  @Test
  public void valueLinks() {
    final Value<Integer> value1 = Values.value();
    final Value<Integer> value2 = Values.value();
    final Value<Integer> value3 = Values.value();
    value3.addValidator(value -> {
      if (value != null && value > 4) {
        throw new IllegalArgumentException();
      }
    });
    final Value<Integer> value4 = Values.value();

    value1.link(value2);
    value2.link(value3);
    value3.link(value4);

    value1.set(1);
    assertEquals(1, value2.get());
    assertEquals(1, value3.get());
    assertEquals(1, value4.get());

    value4.set(2);
    assertEquals(2, value1.get());
    assertEquals(2, value2.get());
    assertEquals(2, value3.get());

    value4.link(value1);
    value3.set(3);
    assertEquals(3, value1.get());
    assertEquals(3, value2.get());
    assertEquals(3, value4.get());

    value2.set(4);
    assertEquals(4, value1.get());
    assertEquals(4, value3.get());
    assertEquals(4, value4.get());

    assertThrows(IllegalArgumentException.class, () -> value1.set(5));
    assertThrows(IllegalArgumentException.class, () -> value2.set(5));
    assertThrows(IllegalArgumentException.class, () -> value3.set(5));
    assertThrows(IllegalArgumentException.class, () -> value4.set(5));
  }
}
