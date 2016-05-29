/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.EventInfoListener;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * User: Björn Darri
 * Date: 24.7.2010
 * Time: 22:24:49
 */
public class DefaultValueMapTest {

  @Test
  public void test() {
    final String key = "key";
    final DefaultValueMap<String, Object> map = new DefaultValueMap<>();
    assertFalse(map.equals(key));
    assertEquals(0, map.size());
    assertFalse(map.containsKey(key));
    map.put(key, null);
    assertTrue(map.containsKey(key));
    assertTrue(map.isValueNull(key));
    assertEquals("", map.getAsString(key));
    map.put(key, key);
    assertFalse(map.isValueNull(key));
    assertEquals(key, map.get(key));
    assertEquals(key, map.getAsString(key));
    assertNull(map.remove("bla"));
    final Object value = map.remove(key);
    assertEquals(value, key);
    assertFalse(map.containsKey(key));
    assertEquals(0, map.size());
    map.put(key, key);
    map.clear();
    assertFalse(map.containsKey(key));
    assertEquals(0, map.size());

    final ValueMap<String, Integer> valueMap = new DefaultValueMap<>();

    final EventInfoListener<ValueChange<String, ?>> valueListener = info -> {
      Assert.assertEquals(key, info.getKey());
      info.toString();
      info.getOldValue();
      info.getNewValue();
      info.isInitialization();
    };
    valueMap.addValueListener(valueListener);

    valueMap.getModifiedObserver();
    valueMap.getValueObserver();

    assertFalse(valueMap.containsKey(key));
    assertTrue(valueMap.originalKeySet().isEmpty());

    valueMap.put(key, 1);
    assertTrue(valueMap.containsKey(key));
    Assert.assertEquals(Integer.valueOf(1), valueMap.get(key));
    Assert.assertEquals(Integer.valueOf(1), valueMap.getOriginal(key));
    assertFalse(valueMap.isValueNull(key));
    assertFalse(valueMap.isModified());
    assertFalse(valueMap.isModified(key));

    valueMap.put(key, 1);
    assertTrue(valueMap.containsKey(key));
    Assert.assertEquals(Integer.valueOf(1), valueMap.get(key));
    assertFalse(valueMap.isModified());
    assertFalse(valueMap.isModified(key));

    valueMap.put(key, 2);
    assertTrue(valueMap.containsKey(key));
    Assert.assertEquals(Integer.valueOf(2), valueMap.get(key));
    Assert.assertEquals(Integer.valueOf(1), valueMap.getOriginal(key));
    assertTrue(valueMap.isModified());
    assertTrue(valueMap.isModified(key));
    assertFalse(valueMap.originalKeySet().isEmpty());

    valueMap.put(key, 3);
    assertTrue(valueMap.containsKey(key));
    Assert.assertEquals(Integer.valueOf(3), valueMap.get(key));
    Assert.assertEquals(Integer.valueOf(1), valueMap.getOriginal(key));
    assertTrue(valueMap.isModified());
    assertTrue(valueMap.isModified(key));

    valueMap.revertAll();
    Assert.assertEquals(Integer.valueOf(1), valueMap.get(key));
    assertFalse(valueMap.isModified());
    assertFalse(valueMap.isModified(key));

    valueMap.put(key, null);
    assertTrue(valueMap.isValueNull(key));
    assertTrue(valueMap.isModified());
    assertTrue(valueMap.isModified(key));
    valueMap.revert(key);
    assertFalse(valueMap.isModified());
    valueMap.revert(key);
    valueMap.put(key, null);

    valueMap.remove(key);
    assertFalse(valueMap.containsKey(key));
    assertFalse(valueMap.isModified());

    valueMap.put(key, 0);
    valueMap.put(key, 1);
    assertTrue(valueMap.isModified());
    assertTrue(valueMap.isModified(key));
    Assert.assertEquals(Integer.valueOf(0), valueMap.getOriginal(key));
    Assert.assertEquals(Integer.valueOf(0), valueMap.getOriginalCopy().get(key));
    valueMap.saveAll();
    assertFalse(valueMap.isModified());
    assertFalse(valueMap.isModified(key));

    valueMap.removeValueListener(valueListener);
    valueMap.removeValueListener(null);
  }

  @Test
  public void setAs() {
    final ValueMap<Integer, String> dest = new DefaultValueMap<>();
    dest.getValueObserver();

    final ValueMap<Integer, String> source = new DefaultValueMap<>();
    source.put(1, "1");
    source.put(2, "2");
    source.put(2, "3");

    dest.setAs(source);

    Assert.assertEquals("1", dest.get(1));
    Assert.assertEquals("3", dest.get(2));
    Assert.assertEquals("2", dest.getOriginal(2));

    assertTrue(dest.equals(source));

    dest.setAs(dest);

    Assert.assertEquals("1", dest.get(1));
    Assert.assertEquals("3", dest.get(2));
    Assert.assertEquals("2", dest.getOriginal(2));

    dest.setAs(null);
    assertFalse(dest.containsKey(1));
    assertFalse(dest.containsKey(2));
    assertFalse(dest.containsKey(3));
  }

  @Test
  public void equals() {
    final ValueMap<String, Integer> mapOne = new DefaultValueMap<>();
    final ValueMap<String, Integer> mapTwo = mapOne.newInstance();
    mapOne.getValueObserver();
    mapTwo.getValueObserver();

    mapOne.put("keyOne", 1);
    mapOne.put("keyTwo", 2);

    assertFalse(mapOne.equals(mapTwo));
    assertFalse(mapTwo.equals(mapOne));

    assertFalse(mapOne.equals(new HashMap()));

    mapTwo.put("keyOne", 1);

    assertFalse(mapOne.equals(mapTwo));
    assertFalse(mapTwo.equals(mapOne));

    mapTwo.put("keyTwo", 2);

    assertTrue(mapOne.equals(mapTwo));
    assertTrue(mapTwo.equals(mapOne));

    mapTwo.put("keyOne", 2);

    assertFalse(mapOne.equals(mapTwo));
    assertFalse(mapTwo.equals(mapOne));

    mapTwo.put("keyOne", 1);
    mapTwo.remove("keyTwo");
    mapTwo.put("keyThree", 3);

    assertFalse(mapOne.equals(mapTwo));
    assertFalse(mapTwo.equals(mapOne));
  }

  @Test
  public void clear() {
    final ValueMap<String, Integer> map = new DefaultValueMap<>();
    map.put("1", 1);
    map.put("2", 2);
    map.put("3", 3);

    map.put("2", 3);

    assertTrue(map.isModified());
    assertTrue(map.isModified("2"));

    map.clear();

    assertTrue(map.values().isEmpty());
    assertTrue(map.keySet().isEmpty());

    assertFalse(map.isModified());
    assertFalse(map.isModified("2"));

    map.put("2", 4);
    assertFalse(map.isModified());
    assertFalse(map.isModified("2"));
  }

  @Test
  public void testHashCode() {
    final ValueMap<String, Integer> map = new DefaultValueMap<>();
    map.put("1", 1);
    map.put("2", 2);
    map.put("3", 3);

    assertEquals(29, map.hashCode());

    map.put("2", null);

    assertEquals(27, map.hashCode());
  }
}
