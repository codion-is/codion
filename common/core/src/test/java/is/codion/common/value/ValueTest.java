/*
 * Copyright (c) 2013 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;

import org.junit.jupiter.api.Test;

import java.util.Objects;
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
    assertEquals(1, value.validators().size());
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
    assertTrue(intValue.optional().isPresent());
    assertTrue(intValue.equalTo(42));
    ValueObserver<Integer> valueObserver = intValue.observer();
    assertFalse(valueObserver.isNullable());
    assertTrue(valueObserver.optional().isPresent());
    assertTrue(valueObserver.equalTo(42));
    EventListener eventListener = eventCounter::incrementAndGet;
    valueObserver.addListener(eventListener);
    valueObserver.addDataListener(data -> {
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
    assertTrue(intValue.optional().isPresent());
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
    assertFalse(value.optional().isPresent());
    value.set("hello");
    assertTrue(value.optional().isPresent());

    valueObserver.removeListener(eventListener);
  }

  @Test
  void linkValues() {
    AtomicInteger modelValueEventCounter = new AtomicInteger();
    Value<Integer> modelValue = Value.propertyValue(this, "integerValue", Integer.class, integerValueChange.observer());
    Value<Integer> uiValue = Value.value();
    uiValue.link(modelValue);

    assertThrows(IllegalStateException.class, () -> uiValue.link(modelValue));

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
    Value<Integer> modelValue = Value.propertyValue(this, "intValue", int.class, integerValueChange.observer());
    Value<Integer> uiValue = Value.value();
    assertFalse(modelValue.isNullable());
    uiValue.link(modelValue.observer());
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
  void propertyValueNoGetter() {
    assertThrows(IllegalArgumentException.class, () -> Value.propertyValue(this, "nonexistent", Integer.class, integerValueChange.observer()));
  }

  @Test
  void propertyValueNoOwner() {
    assertThrows(NullPointerException.class, () -> Value.propertyValue(null, "integerValue", Integer.class, integerValueChange.observer()));
  }

  @Test
  void propertyValueNoPropertyName() {
    assertThrows(IllegalArgumentException.class, () -> Value.propertyValue(this, null, Integer.class, integerValueChange.observer()));
  }

  @Test
  void propertyValueNoValueClass() {
    assertThrows(NullPointerException.class, () -> Value.propertyValue(this, "integerValue", null, integerValueChange.observer()));
  }

  @Test
  void setReadOnlyPropertyValue() {
    Value<Integer> modelValue = Value.propertyValue(this, "intValue", Integer.class, integerValueChange.observer());
    assertThrows(IllegalStateException.class, () -> modelValue.set(43));
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

    assertThrows(IllegalStateException.class, () -> value1.link(value2));

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

    assertThrows(IllegalStateException.class, () -> value.unlink(originalValue));

    ValueObserver<Integer> originalValueObserver = originalValue.observer();

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

  @Test
  void weakListeners() {
    Value<Integer> value = Value.value();
    ValueObserver<Integer> observer = value.observer();
    EventListener listener = () -> {};
    EventDataListener<Integer> dataListener = integer -> {};
    observer.addWeakListener(listener);
    observer.addWeakListener(listener);
    observer.addWeakDataListener(dataListener);
    observer.addWeakDataListener(dataListener);
    value.set(1);
    observer.removeWeakListener(listener);
    observer.removeWeakDataListener(dataListener);
  }
}
