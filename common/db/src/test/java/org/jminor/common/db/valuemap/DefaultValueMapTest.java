/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.valuemap;

import org.jminor.common.db.valuemap.exception.NullValidationException;
import org.jminor.common.db.valuemap.exception.ValidationException;
import org.jminor.common.event.EventDataListener;
import org.jminor.common.event.EventListener;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * User: Björn Darri
 * Date: 24.7.2010
 * Time: 22:24:49
 */
public class DefaultValueMapTest {

  @Test
  public void test() {
    final String attr1 = "1";
    final String attr2 = "2";
    final DefaultValueMap<String, Object> map = new DefaultValueMap<>();
    assertNotEquals(map, attr1);
    assertEquals(0, map.size());
    assertFalse(map.containsKey(attr1));
    assertFalse(map.isNotNull(attr1));
    map.put(attr1, null);
    assertTrue(map.containsKey(attr1));
    assertTrue(map.isNull(attr1));
    assertFalse(map.isNotNull(attr1));
    assertEquals("", map.getAsString(attr1));
    map.put(attr1, attr1);
    assertFalse(map.isNull(attr1));
    assertTrue(map.isNotNull(attr1));
    assertEquals(attr1, map.get(attr1));
    assertNull(map.remove(attr2));
    final Object value = map.remove(attr1);
    assertEquals(value, attr1);
    assertFalse(map.containsKey(attr1));
    assertEquals(0, map.size());
    map.put(attr1, attr1);
    map.clear();
    assertFalse(map.containsKey(attr1));
    assertEquals(0, map.size());

    final ValueMap<String, Integer> valueMap = new DefaultValueMap<>();

    final EventDataListener<ValueChange<String, Integer>> valueListener = valueChange -> {
      assertEquals(attr1, valueChange.getKey());
      valueChange.toString();
      valueChange.getPreviousValue();
      valueChange.getCurrentValue();
      valueChange.isInitialization();
    };
    valueMap.addValueListener(valueListener);

    valueMap.getModifiedObserver();
    valueMap.getValueObserver();

    assertFalse(valueMap.containsKey(attr1));
    assertTrue(valueMap.originalKeySet().isEmpty());

    valueMap.put(attr1, 1);
    assertTrue(valueMap.containsKey(attr1));
    assertEquals(Integer.valueOf(1), valueMap.get(attr1));
    assertEquals(Integer.valueOf(1), valueMap.getOriginal(attr1));
    assertFalse(valueMap.isNull(attr1));
    assertTrue(valueMap.isNotNull(attr1));
    assertFalse(valueMap.isModified());
    assertFalse(valueMap.isModified(attr1));

    valueMap.put(attr1, 1);
    assertTrue(valueMap.containsKey(attr1));
    assertEquals(Integer.valueOf(1), valueMap.get(attr1));
    assertFalse(valueMap.isModified());
    assertFalse(valueMap.isModified(attr1));

    valueMap.put(attr1, 2);
    assertTrue(valueMap.containsKey(attr1));
    assertEquals(Integer.valueOf(2), valueMap.get(attr1));
    assertEquals(Integer.valueOf(1), valueMap.getOriginal(attr1));
    assertTrue(valueMap.isModified());
    assertTrue(valueMap.isModified(attr1));
    assertFalse(valueMap.originalKeySet().isEmpty());

    valueMap.put(attr1, 3);
    assertTrue(valueMap.containsKey(attr1));
    assertEquals(Integer.valueOf(3), valueMap.get(attr1));
    assertEquals(Integer.valueOf(1), valueMap.getOriginal(attr1));
    assertTrue(valueMap.isModified());
    assertTrue(valueMap.isModified(attr1));

    valueMap.revertAll();
    assertEquals(Integer.valueOf(1), valueMap.get(attr1));
    assertFalse(valueMap.isModified());
    assertFalse(valueMap.isModified(attr1));

    valueMap.put(attr1, null);
    assertTrue(valueMap.isNull(attr1));
    assertTrue(valueMap.isModified());
    assertTrue(valueMap.isModified(attr1));
    valueMap.revert(attr1);
    assertFalse(valueMap.isModified());
    valueMap.revert(attr1);
    valueMap.put(attr1, null);

    valueMap.remove(attr1);
    assertFalse(valueMap.containsKey(attr1));
    assertFalse(valueMap.isModified());

    valueMap.put(attr1, 0);
    valueMap.put(attr1, 1);
    assertTrue(valueMap.isModified());
    assertTrue(valueMap.isModified(attr1));
    assertEquals(Integer.valueOf(0), valueMap.getOriginal(attr1));
    valueMap.saveAll();
    assertFalse(valueMap.isModified());
    assertFalse(valueMap.isModified(attr1));

    valueMap.removeValueListener(valueListener);
    valueMap.removeValueListener(null);
  }

  @Test
  public void setAs() {
    final String one = "one";
    final String two = "two";
    final String three = "three";

    final ValueMap<String, String> dest = new DefaultValueMap<>();
    dest.getValueObserver();

    final ValueMap<String, String> source = new DefaultValueMap<>();
    source.put(one, "1");
    source.put(two, "2");
    source.put(two, "3");

    dest.setAs(source);

    assertEquals("1", dest.get(one));
    assertEquals("3", dest.get(two));
    assertEquals("2", dest.getOriginal(two));

    assertEquals(dest, source);

    dest.setAs(dest);

    assertEquals("1", dest.get(one));
    assertEquals("3", dest.get(two));
    assertEquals("2", dest.getOriginal(two));

    dest.setAs(null);
    assertFalse(dest.containsKey(one));
    assertFalse(dest.containsKey(two));
    assertFalse(dest.containsKey(three));
  }

  @Test
  public void equals() {
    final ValueMap<String, Integer> mapOne = new DefaultValueMap<>();
    final ValueMap<String, Integer> mapTwo = new DefaultValueMap<>();
    mapOne.getValueObserver();
    mapTwo.getValueObserver();

    final String one = "one";
    final String two = "two";
    final String three = "three";

    mapOne.put(one, 1);
    mapOne.put(two, 2);

    assertNotEquals(mapOne, mapTwo);
    assertNotEquals(mapTwo, mapOne);

    assertNotEquals(mapOne, new HashMap());

    mapTwo.put(one, 1);

    assertNotEquals(mapOne, mapTwo);
    assertNotEquals(mapTwo, mapOne);

    mapTwo.put(two, 2);

    assertEquals(mapOne, mapTwo);
    assertEquals(mapTwo, mapOne);

    mapTwo.put(one, 2);

    assertNotEquals(mapOne, mapTwo);
    assertNotEquals(mapTwo, mapOne);

    mapTwo.put(one, 1);
    mapTwo.remove(two);
    mapTwo.put(three, 3);

    assertNotEquals(mapOne, mapTwo);
    assertNotEquals(mapTwo, mapOne);
  }

  @Test
  public void clear() {
    final String one = "one";
    final String two = "two";
    final String three = "three";

    final ValueMap<String, Integer> map = new DefaultValueMap<>();
    map.put(one, 1);
    map.put(two, 2);
    map.put(three, 3);

    map.put(two, 3);

    assertTrue(map.isModified());
    assertTrue(map.isModified(two));

    map.clear();

    assertTrue(map.values().isEmpty());
    assertTrue(map.keySet().isEmpty());

    assertFalse(map.isModified());
    assertFalse(map.isModified(two));

    map.put(two, 4);
    assertFalse(map.isModified());
    assertFalse(map.isModified(two));
  }

  @Test
  public void testHashCode() {
    final String one = "one";
    final String two = "two";
    final String three = "three";

    final ValueMap<String, Integer> map = new DefaultValueMap<>();
    map.put(one, 1);
    map.put(two, 2);
    map.put(three, 3);

    assertEquals(29, map.hashCode());

    map.put(two, null);

    assertEquals(27, map.hashCode());
  }

  @Test
  public void nullValidation() throws ValidationException {
    final String testAttribute = "test";
    final ValueMap.Validator<String, ValueMap<String, Integer>> validator =
            new DefaultValueMapValidator<String, ValueMap<String, Integer>>() {
              @Override
              public boolean isNullable(final ValueMap<String, Integer> valueMap, final String key) {
                return super.isNullable(valueMap, key) && !key.equals(testAttribute);
              }
            };
    final ValueMap<String, Integer> map = new DefaultValueMap<>();
    map.put(testAttribute, null);
    assertThrows(NullValidationException.class, () -> validator.validate(map));
    map.put(testAttribute, 1);
    validator.validate(map);
    map.put(testAttribute, null);
    assertThrows(NullValidationException.class, () -> validator.validate(map));
  }

  @Test
  public void isValid() {
    final String testAttribute = "test";
    final DefaultValueMapValidator<String, ValueMap<String, Integer>> validator =
            new DefaultValueMapValidator<String, ValueMap<String, Integer>>() {
              @Override
              public void validate(final ValueMap<String, Integer> valueMap, final String key) throws ValidationException {
                final Integer value = valueMap.get(testAttribute);
                if (value.equals(1)) {
                  throw new ValidationException(testAttribute, 1, "Invalid");
                }
              }
            };
    final ValueMap<String, Integer> map = new DefaultValueMap<>();
    map.put(testAttribute, 0);
    assertTrue(validator.isValid(map));
    map.put(testAttribute, 1);
    assertFalse(validator.isValid(map));
  }

  @Test
  public void validate() throws ValidationException {
    final String testAttribute = "test";
    final DefaultValueMapValidator<String, ValueMap<String, Integer>> validator =
            new DefaultValueMapValidator<String, ValueMap<String, Integer>>() {
              @Override
              public void validate(final ValueMap<String, Integer> valueMap, final String key) throws ValidationException {
                super.validate(valueMap, key);
                throw new ValidationException(testAttribute, valueMap.get(testAttribute), "Invalid");
              }
            };
    final ValueMap<String, Integer> map = new DefaultValueMap<>();
    map.put(testAttribute, 1);

    assertThrows(ValidationException.class, () -> validator.validate(map));
  }

  @Test
  public void revalidate() {
    final AtomicInteger counter = new AtomicInteger();
    final DefaultValueMapValidator<String, ValueMap<String, Integer>> validator = new DefaultValueMapValidator<>();
    final EventListener listener = counter::incrementAndGet;
    validator.addRevalidationListener(listener);
    validator.revalidate();
    assertEquals(1, counter.get());
    validator.removeRevalidationListener(listener);
    validator.revalidate();
    assertEquals(1, counter.get());
  }
}
