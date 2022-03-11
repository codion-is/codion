/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ValueTest {

  private final Event<Integer> integerValueChange = Event.event();
  private Integer integerValue = 42;
  private Double doubleValue = 42d;

  public Integer getIntegerValue() {
    return integerValue;
  }

  public void setIntegerValue(Integer integerValue) {
    if (!Objects.equals(this.integerValue, integerValue)) {
      this.integerValue = integerValue;
      integerValueChange.onEvent(integerValue);
    }
  }

  private Double getDoubleValue() {
    return doubleValue;
  }

  private void setDoubleValue(Double value) {
    if (value == null) {
      throw new IllegalArgumentException("Non-null");
    }
    doubleValue = value;
  }

  public int getIntValue() {
    return integerValue;
  }

  @Test
  void valueSetterGetter() {
    Value<Boolean> changeEventFired = Value.value(false);
    Value<Double> value = Value.value(this::getDoubleValue, this::setDoubleValue);
    value.addListener(() -> changeEventFired.set(true));
    assertEquals(42d, value.get());
    assertThrows(IllegalArgumentException.class, () -> value.set(null));
    assertFalse(changeEventFired.get());
    value.set(1d);
    assertTrue(changeEventFired.get());
    assertEquals(1d, doubleValue);
    assertEquals(1d, value.get());
  }

  @Test
  void validator() {
    Value.Validator<Integer> validator = value -> {
      if (value != null && value > 10) {
        throw new IllegalArgumentException();
      }
    };
    Value<Integer> value = Value.value(0, 0);
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
  void value() {
    AtomicInteger eventCounter = new AtomicInteger();
    Value<Integer> intValue = Value.value(42, -1);
    assertFalse(intValue.isNullable());
    assertTrue(intValue.toOptional().isPresent());
    assertTrue(intValue.equalTo(42));
    ValueObserver<Integer> valueObserver = intValue.getObserver();
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
    assertTrue(intValue.equalTo(20));
    intValue.set(null);
    assertTrue(intValue.equalTo(-1));
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

    Value<String> stringValue = Value.value(null, "null");
    assertFalse(stringValue.isNullable());
    assertEquals("null", stringValue.get());
    stringValue.set("test");
    assertEquals("test", stringValue.get());
    stringValue.set(null);
    assertEquals("null", stringValue.get());

    Value<String> value = Value.value();
    assertFalse(value.toOptional().isPresent());
    value.set("hello");
    assertTrue(value.toOptional().isPresent());
  }

  @Test
  void linkValues() {
    AtomicInteger modelValueEventCounter = new AtomicInteger();
    Value<Integer> modelValue = Value.propertyValue(this, "integerValue", Integer.class, integerValueChange.getObserver());
    Value<Integer> uiValue = Value.value();
    uiValue.link(modelValue);

    assertThrows(IllegalArgumentException.class, () -> uiValue.link(modelValue));

    modelValue.addListener(modelValueEventCounter::incrementAndGet);
    AtomicInteger uiValueEventCounter = new AtomicInteger();
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
    assertTrue(modelValue.equalTo(null));
    assertNull(uiValue.get());
    assertTrue(uiValue.isNull());
    assertEquals(3, modelValueEventCounter.get());
    assertEquals(3, uiValueEventCounter.get());

    Value<Integer> valueOne = Value.value();
    assertThrows(IllegalArgumentException.class, () -> valueOne.link(valueOne));
  }

  @Test
  void linkValuesReadOnly() {
    AtomicInteger modelValueEventCounter = new AtomicInteger();
    Value<Integer> modelValue = Value.propertyValue(this, "intValue", int.class, integerValueChange.getObserver());
    Value<Integer> uiValue = Value.value();
    assertFalse(modelValue.isNullable());
    uiValue.link(modelValue.getObserver());
    modelValue.addListener(modelValueEventCounter::incrementAndGet);
    AtomicInteger uiValueEventCounter = new AtomicInteger();
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
  void getOrThrow() {
    PropertyValue<Integer> integerValue = Value.propertyValue(this, "integerValue", Integer.class, integerValueChange.getObserver());
    integerValue.set(null);
    assertThrows(IllegalStateException.class, integerValue::getOrThrow);
  }

  @Test
  void propertyValueNoGetter() {
    assertThrows(IllegalArgumentException.class, () -> Value.propertyValue(this, "nonexistent", Integer.class, integerValueChange.getObserver()));
  }

  @Test
  void propertyValueNoOwner() {
    assertThrows(NullPointerException.class, () -> Value.propertyValue(null, "integerValue", Integer.class, integerValueChange.getObserver()));
  }

  @Test
  void propertyValueNoPropertyName() {
    assertThrows(IllegalArgumentException.class, () -> Value.propertyValue(this, null, Integer.class, integerValueChange.getObserver()));
  }

  @Test
  void propertyValueNoValueClass() {
    assertThrows(NullPointerException.class, () -> Value.propertyValue(this, "integerValue", null, integerValueChange.getObserver()));
  }

  @Test
  void setReadOnlyPropertyValue() {
    Value<Integer> modelValue = Value.propertyValue(this, "intValue", Integer.class, integerValueChange.getObserver());
    assertThrows(IllegalStateException.class, () -> modelValue.set(43));
  }

  @Test
  void valueSet() {
    ValueSet<Integer> valueSet = Value.valueSet();
    assertTrue(valueSet.isEmpty());
    assertFalse(valueSet.isNotEmpty());

    assertFalse(valueSet.isNullable());
    assertTrue(valueSet.toOptional().isPresent());

    assertTrue(valueSet.add(1));
    assertFalse(valueSet.add(1));
    assertTrue(valueSet.remove(1));
    assertFalse(valueSet.remove(1));

    Set<Integer> initialValues = new HashSet<>();
    initialValues.add(1);
    initialValues.add(2);

    valueSet = Value.valueSet(initialValues);
    assertFalse(valueSet.isEmpty());
    assertTrue(valueSet.isNotEmpty());
    assertEquals(initialValues, valueSet.get());
    assertTrue(valueSet.equalTo(initialValues));

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

    Value<Integer> value = valueSet.value();

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
  void valueSetEvents() {
    ValueSet<Integer> valueSet = Value.valueSet();
    Value<Integer> value = valueSet.value();

    AtomicInteger valueEventCounter = new AtomicInteger();
    EventDataListener<Integer> listener = integer -> valueEventCounter.incrementAndGet();
    value.addDataListener(listener);

    AtomicInteger valueSetEventCounter = new AtomicInteger();
    EventDataListener<Set<Integer>> setListener = integers -> valueSetEventCounter.incrementAndGet();
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
  void valueLinks() {
    Value<Integer> value1 = Value.value();
    Value<Integer> value2 = Value.value();
    Value<Integer> value3 = Value.value();
    value3.addValidator(value -> {
      if (value != null && value > 4) {
        throw new IllegalArgumentException();
      }
    });
    Value<Integer> value4 = Value.value();

    value1.link(value2);

    assertThrows(IllegalArgumentException.class, () -> value1.link(value2));

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

    assertThrows(IllegalStateException.class, () -> value4.link(value1));

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

  @Test
  void valueAsEventDataListener() {
    Value<Integer> value = Value.value();
    Value<Integer> listeningValue = Value.value();

    value.addDataListener(listeningValue);
    value.set(1);

    assertEquals(1, listeningValue.get());

    listeningValue.set(2);

    value.set(3);

    assertEquals(3, listeningValue.get());
  }

  @Test
  void unlink() {
    Value<Integer> value = Value.value(0, 0);
    value.addValidator(integer -> {
      if (integer > 2) {
        throw new IllegalArgumentException();
      }
    });
    Value<Integer> originalValue = Value.value(1);

    value.link(originalValue);
    assertEquals(originalValue.get(), value.get());

    assertThrows(IllegalArgumentException.class, () -> originalValue.set(3));

    value.unlink(originalValue);

    originalValue.set(3);
    assertNotEquals(originalValue.get(), value.get());
    assertEquals(1, value.get());

    assertThrows(IllegalArgumentException.class, () -> value.unlink(originalValue));

    ValueObserver<Integer> originalValueObserver = originalValue.getObserver();

    assertThrows(IllegalArgumentException.class, () -> value.link(originalValueObserver));

    originalValue.set(2);

    value.link(originalValueObserver);
    assertEquals(originalValue.get(), value.get());

    assertThrows(IllegalArgumentException.class, () -> originalValue.set(3));

    value.unlink(originalValueObserver);
    value.unlink(originalValueObserver);

    originalValue.set(3);

    assertNotEquals(originalValue.get(), value.get());
    assertEquals(2, value.get());
  }
}
