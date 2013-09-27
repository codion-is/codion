/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.model.EventInfoListener;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
    assertFalse(map.containsValue(key));
    map.setValue(key, null);
    assertTrue(map.containsValue(key));
    assertTrue(map.isValueNull(key));
    assertEquals("", map.getValueAsString(key));
    map.setValue(key, key);
    assertFalse(map.isValueNull(key));
    assertEquals(key, map.getValue(key));
    assertEquals(key, map.getValueAsString(key));
    assertNull(map.removeValue("bla"));
    final Object value = map.removeValue(key);
    assertEquals(value, key);
    assertFalse(map.containsValue(key));
    assertEquals(0, map.size());
    map.setValue(key, key);
    map.clear();
    assertFalse(map.containsValue(key));
    assertEquals(0, map.size());

    final ValueMap<String, Integer> valueMap = new DefaultValueMap<>();

    final EventInfoListener<ValueChange> valueListener = new EventInfoListener<ValueChange>() {
      @Override
      public void eventOccurred(final ValueChange info) {
        Assert.assertEquals(key, info.getKey());
        info.getOldValue();
        info.getNewValue();
        info.isInitialization();
        info.isNewValueEqual(null);
        info.isNewValueNull();
        info.isOldValueEqual(null);
        info.isOldValueNull();
      }
    };
    valueMap.addValueListener(valueListener);

    valueMap.getModifiedState();
    valueMap.getValueChangeObserver();

    assertFalse(valueMap.containsValue(key));
    assertTrue(valueMap.getOriginalValueKeys().isEmpty());

    valueMap.setValue(key, 1);
    assertTrue(valueMap.containsValue(key));
    Assert.assertEquals(Integer.valueOf(1), valueMap.getValue(key));
    Assert.assertEquals(Integer.valueOf(1), valueMap.getOriginalValue(key));
    assertFalse(valueMap.isValueNull(key));
    assertFalse(valueMap.isModified());
    assertFalse(valueMap.isModified(key));

    valueMap.setValue(key, 1);
    assertTrue(valueMap.containsValue(key));
    Assert.assertEquals(Integer.valueOf(1), valueMap.getValue(key));
    assertFalse(valueMap.isModified());
    assertFalse(valueMap.isModified(key));

    valueMap.setValue(key, 2);
    assertTrue(valueMap.containsValue(key));
    Assert.assertEquals(Integer.valueOf(2), valueMap.getValue(key));
    Assert.assertEquals(Integer.valueOf(1), valueMap.getOriginalValue(key));
    assertTrue(valueMap.isModified());
    assertTrue(valueMap.isModified(key));
    assertFalse(valueMap.getOriginalValueKeys().isEmpty());

    valueMap.setValue(key, 3);
    assertTrue(valueMap.containsValue(key));
    Assert.assertEquals(Integer.valueOf(3), valueMap.getValue(key));
    Assert.assertEquals(Integer.valueOf(1), valueMap.getOriginalValue(key));
    assertTrue(valueMap.isModified());
    assertTrue(valueMap.isModified(key));

    valueMap.revertAll();
    Assert.assertEquals(Integer.valueOf(1), valueMap.getValue(key));
    assertFalse(valueMap.isModified());
    assertFalse(valueMap.isModified(key));

    valueMap.setValue(key, null);
    assertTrue(valueMap.isValueNull(key));
    assertTrue(valueMap.isModified());
    assertTrue(valueMap.isModified(key));

    valueMap.removeValue(key);
    assertFalse(valueMap.containsValue(key));
    assertFalse(valueMap.isModified());

    valueMap.setValue(key, 0);
    valueMap.setValue(key, 1);
    assertTrue(valueMap.isModified());
    assertTrue(valueMap.isModified(key));
    Assert.assertEquals(Integer.valueOf(0), valueMap.getOriginalValue(key));
    Assert.assertEquals(Integer.valueOf(0), valueMap.getOriginalCopy().getValue(key));
    valueMap.saveAll();
    assertFalse(valueMap.isModified());
    assertFalse(valueMap.isModified(key));

    valueMap.removeValueListener(valueListener);
  }

  @Test
  public void setAs() {
    final ValueMap<Integer, String> dest = new DefaultValueMap<>();

    final ValueMap<Integer, String> source = new DefaultValueMap<>();
    source.setValue(1, "1");
    source.setValue(2, "2");
    source.setValue(2, "3");

    dest.setAs(source);

    Assert.assertEquals("1", dest.getValue(1));
    Assert.assertEquals("3", dest.getValue(2));
    Assert.assertEquals("2", dest.getOriginalValue(2));

    assertTrue(dest.equals(source));

    dest.setAs(dest);

    Assert.assertEquals("1", dest.getValue(1));
    Assert.assertEquals("3", dest.getValue(2));
    Assert.assertEquals("2", dest.getOriginalValue(2));
  }

  @Test
  public void equals() {
    final ValueMap<String, Integer> mapOne = new DefaultValueMap<>();
    final ValueMap<String, Integer> mapTwo = mapOne.getInstance();
    mapOne.getValueChangeObserver();
    mapTwo.getValueChangeObserver();

    mapOne.setValue("keyOne", 1);
    mapOne.setValue("keyTwo", 2);

    assertFalse(mapOne.equals(mapTwo));
    assertFalse(mapTwo.equals(mapOne));

    assertFalse(mapOne.equals(new HashMap()));

    mapTwo.setValue("keyOne", 1);

    assertFalse(mapOne.equals(mapTwo));
    assertFalse(mapTwo.equals(mapOne));

    mapTwo.setValue("keyTwo", 2);

    assertTrue(mapOne.equals(mapTwo));
    assertTrue(mapTwo.equals(mapOne));

    mapTwo.setValue("keyOne", 2);

    assertFalse(mapOne.equals(mapTwo));
    assertFalse(mapTwo.equals(mapOne));

    mapTwo.setValue("keyOne", 1);
    mapTwo.removeValue("keyTwo");
    mapTwo.setValue("keyThree", 3);

    assertFalse(mapOne.equals(mapTwo));
    assertFalse(mapTwo.equals(mapOne));
  }

  @Test
  public void clear() {
    final ValueMap<String, Integer> map = new DefaultValueMap<>();
    map.setValue("1", 1);
    map.setValue("2", 2);
    map.setValue("3", 3);

    map.setValue("2", 3);

    assertTrue(map.isModified());
    assertTrue(map.isModified("2"));

    map.clear();

    assertTrue(map.getValues().isEmpty());
    assertTrue(map.getValueKeys().isEmpty());

    assertFalse(map.isModified());
    assertFalse(map.isModified("2"));

    map.setValue("2", 4);
    assertFalse(map.isModified());
    assertFalse(map.isModified("2"));
  }

  @Test
  public void testHashCode() {
    final ValueMap<String, Integer> map = new DefaultValueMap<>();
    map.setValue("1", 1);
    map.setValue("2", 2);
    map.setValue("3", 3);

    assertEquals(29, map.hashCode());

    map.setValue("2", null);

    assertEquals(27, map.hashCode());
  }
}
