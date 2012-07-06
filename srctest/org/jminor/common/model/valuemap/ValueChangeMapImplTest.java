/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.junit.Test;

import java.awt.event.ActionEvent;
import java.util.HashMap;

import static org.junit.Assert.*;

public class ValueChangeMapImplTest {

  @Test
  public void setAs() {
    final ValueChangeMap<Integer, String> dest = new ValueChangeMapImpl<Integer, String>();

    final ValueChangeMap<Integer, String> source = new ValueChangeMapImpl<Integer, String>();
    source.setValue(1, "1");
    source.setValue(2, "2");
    source.setValue(2, "3");

    dest.setAs(source);

    assertEquals("1", dest.getValue(1));
    assertEquals("3", dest.getValue(2));
    assertEquals("2", dest.getOriginalValue(2));

    assertTrue(dest.equals(source));

    dest.setAs(dest);

    assertEquals("1", dest.getValue(1));
    assertEquals("3", dest.getValue(2));
    assertEquals("2", dest.getOriginalValue(2));
  }

  @Test
  public void test() {
    final ValueChangeMap<String, Integer> valueChangeMap = new ValueChangeMapImpl<String, Integer>();
    final String key = "key";

    final ValueChangeListener<String, Integer> valueListener = new ValueChangeListener<String, Integer>() {
      @Override
      protected void valueChanged(final ValueChangeEvent<String, Integer> event) {
        assertEquals(key, event.getKey());
        assertEquals(valueChangeMap, event.getValueOwner());
        event.getOldValue();
        event.getNewValue();
        event.isInitialization();
        event.isModelChange();
        event.isNewValueEqual(null);
        event.isNewValueNull();
        event.isOldValueEqual(null);
        event.isOldValueNull();
        event.isUIChange();
      }
    };
    try {
      valueListener.eventOccurred(new ActionEvent(null, -1, null));
      fail("ValueChangeListener only works with ValueChangeEvent");
    }
    catch (IllegalArgumentException e) {}
    valueChangeMap.addValueListener(valueListener);

    valueChangeMap.getModifiedState();
    valueChangeMap.getValueChangeObserver();

    assertFalse(valueChangeMap.containsValue(key));
    assertTrue(valueChangeMap.getOriginalValueKeys().isEmpty());

    valueChangeMap.setValue(key, 1);
    assertTrue(valueChangeMap.containsValue(key));
    assertEquals(Integer.valueOf(1), valueChangeMap.getValue(key));
    assertEquals(Integer.valueOf(1), valueChangeMap.getOriginalValue(key));
    assertFalse(valueChangeMap.isValueNull(key));
    assertFalse(valueChangeMap.isModified());
    assertFalse(valueChangeMap.isModified(key));

    valueChangeMap.setValue(key, 1);
    assertTrue(valueChangeMap.containsValue(key));
    assertEquals(Integer.valueOf(1), valueChangeMap.getValue(key));
    assertFalse(valueChangeMap.isModified());
    assertFalse(valueChangeMap.isModified(key));

    valueChangeMap.setValue(key, 2);
    assertTrue(valueChangeMap.containsValue(key));
    assertEquals(Integer.valueOf(2), valueChangeMap.getValue(key));
    assertEquals(Integer.valueOf(1), valueChangeMap.getOriginalValue(key));
    assertTrue(valueChangeMap.isModified());
    assertTrue(valueChangeMap.isModified(key));
    assertFalse(valueChangeMap.getOriginalValueKeys().isEmpty());

    valueChangeMap.setValue(key, 3);
    assertTrue(valueChangeMap.containsValue(key));
    assertEquals(Integer.valueOf(3), valueChangeMap.getValue(key));
    assertEquals(Integer.valueOf(1), valueChangeMap.getOriginalValue(key));
    assertTrue(valueChangeMap.isModified());
    assertTrue(valueChangeMap.isModified(key));

    valueChangeMap.revertAll();
    assertEquals(Integer.valueOf(1), valueChangeMap.getValue(key));
    assertFalse(valueChangeMap.isModified());
    assertFalse(valueChangeMap.isModified(key));

    valueChangeMap.setValue(key, null);
    assertTrue(valueChangeMap.isValueNull(key));
    assertTrue(valueChangeMap.isModified());
    assertTrue(valueChangeMap.isModified(key));

    valueChangeMap.removeValue(key);
    assertFalse(valueChangeMap.containsValue(key));
    assertFalse(valueChangeMap.isModified());

    valueChangeMap.setValue(key, 0);
    valueChangeMap.setValue(key, 1);
    assertTrue(valueChangeMap.isModified());
    assertTrue(valueChangeMap.isModified(key));
    assertEquals(Integer.valueOf(0), valueChangeMap.getOriginalValue(key));
    assertEquals(Integer.valueOf(0), valueChangeMap.getOriginalCopy().getValue(key));
    valueChangeMap.saveAll();
    assertFalse(valueChangeMap.isModified());
    assertFalse(valueChangeMap.isModified(key));

    valueChangeMap.removeValueListener(valueListener);
  }

  @Test
  public void equals() {
    final ValueChangeMap<String, Integer> mapOne = new ValueChangeMapImpl<String, Integer>();
    final ValueChangeMap<String, Integer> mapTwo = mapOne.getInstance();
    mapOne.getValueChangeObserver();
    mapTwo.getValueChangeObserver();

    mapOne.initializeValue("keyOne", 1);
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
    final ValueChangeMap<String, Integer> map = new ValueChangeMapImpl<String, Integer>();
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
}
