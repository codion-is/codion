/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2013 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.common.value;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class ValueTest {

  private Double doubleValue = 42d;

  private Double getDoubleValue() {
    return doubleValue;
  }

  private void setDoubleValue(Double value) {
    if (value == null) {
      throw new IllegalArgumentException("Non-null");
    }
    doubleValue = value;
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
    assertTrue(value.addValidator(validator));
    assertFalse(value.addValidator(validator));
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
    assertFalse(intValue.nullable());
    assertTrue(intValue.optional().isPresent());
    assertTrue(intValue.equalTo(42));
    ValueObserver<Integer> valueObserver = intValue.observer();
    assertFalse(valueObserver.nullable());
    assertTrue(valueObserver.optional().isPresent());
    assertTrue(valueObserver.equalTo(42));
    Runnable listener = eventCounter::incrementAndGet;
    assertTrue(valueObserver.addListener(listener));
    assertFalse(valueObserver.addListener(listener));
    valueObserver.addDataListener(data -> {
      if (eventCounter.get() != 2) {
        assertNotNull(data);
      }
    });
    assertFalse(intValue.set(42));
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
    assertFalse(stringValue.nullable());
    assertEquals("null", stringValue.get());
    stringValue.set("test");
    assertEquals("test", stringValue.get());
    stringValue.set(null);
    assertEquals("null", stringValue.get());

    Value<String> value = Value.value();
    assertFalse(value.optional().isPresent());
    value.set("hello");
    assertTrue(value.optional().isPresent());

    assertTrue(valueObserver.removeListener(listener));
    assertFalse(valueObserver.removeListener(listener));
  }

  @Test
  void linkValues() {
    AtomicInteger modelValueEventCounter = new AtomicInteger();
    Value<Integer> modelValue = Value.value(42);
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
    Value<Integer> modelValue = Value.value(42, 0);
    Value<Integer> uiValue = Value.value();
    assertFalse(modelValue.nullable());
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

    modelValue.set(22);
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
  void exceptionalValue() {
    Value<Integer> value1 = new AbstractValue<Integer>() {
      int intValue = 0;
      @Override
      protected void setValue(Integer value) {
        if (value == -1) {
          throw new RuntimeException();
        }
        intValue = value;
      }
      @Override
      public Integer get() {
        return intValue;
      }
    };

    Value<Integer> value2 = Value.value();
    value2.link(value1);
    value2.set(1);
    assertEquals(1, value1.get());

    assertThrows(RuntimeException.class, () -> value2.set(-1));
    assertEquals(1, value2.get());
  }

  @Test
  void valueAsDataListener() {
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
    Runnable listener = () -> {};
    Consumer<Integer> dataListener = integer -> {};
    observer.addWeakListener(listener);
    observer.addWeakListener(listener);
    observer.addWeakDataListener(dataListener);
    observer.addWeakDataListener(dataListener);
    value.set(1);
    observer.removeWeakListener(listener);
    observer.removeWeakDataListener(dataListener);
  }

  @Test
  void setEqual() {
    class Test {

      final int value;
      final String text;

      Test(int value, String text) {
        this.value = value;
        this.text = text;
      }

      @Override
      public boolean equals(Object obj) {
        return obj instanceof Test && ((Test) obj).text.equals(text);
      }
    }
    Test test1 = new Test(1, "Hello");
    Test test2 = new Test(2, "Hello");
    Value<Test> value = Value.value(test1);
    value.addListener(() -> {
      throw new RuntimeException("Change event should not have been triggered");
    });
    value.set(test2);
    assertSame(test2, value.get());
  }

  @Test
  void map() {
    Value<Integer> value = Value.value(0);
    Function<Integer, Integer> increment = currentValue -> currentValue + 1;
    value.map(increment);
    assertEquals(1, value.get());
    value.map(currentValue -> null);
    assertTrue(value.isNull());
    value.map(currentValue -> 42);
    assertTrue(value.isNotNull());
  }
}
