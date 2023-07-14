/*
 * Copyright (c) 2013 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

import is.codion.common.event.EventDataListener;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ValueSetTest {

  @Test
  void valueSet() {
    ValueSet<Integer> valueSet = ValueSet.valueSet();
    assertTrue(valueSet.isEmpty());
    assertFalse(valueSet.isNotEmpty());

    assertFalse(valueSet.isNullable());
    assertFalse(valueSet.isNull());
    assertTrue(valueSet.optional().isPresent());

    assertTrue(valueSet.add(1));
    assertFalse(valueSet.add(1));
    assertTrue(valueSet.remove(1));
    assertFalse(valueSet.remove(1));

    Set<Integer> initialValues = new HashSet<>();
    initialValues.add(1);
    initialValues.add(2);

    valueSet = ValueSet.valueSet(initialValues);
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
    ValueSet<Integer> valueSet = ValueSet.valueSet();
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
}