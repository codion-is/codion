/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import static org.junit.Assert.*;
import org.junit.Test;

import java.util.HashMap;

public class ValueChangeMapImplTest {

  @Test
  public void test() {
    final ValueChangeMap<String, Integer> model = new ValueChangeMapImpl<String, Integer>();

    model.stateModified();
    model.eventValueChanged();

    final String key = "key";
    assertFalse(model.containsValue(key));

    model.setValue(key, 1);
    assertTrue(model.containsValue(key));
    assertEquals(Integer.valueOf(1), model.getValue(key));
    assertEquals(Integer.valueOf(1), model.getOriginalValue(key));
    assertFalse(model.isValueNull(key));
    assertFalse(model.isModified());
    assertFalse(model.isModified(key));

    model.setValue(key, 1);
    assertTrue(model.containsValue(key));
    assertEquals(Integer.valueOf(1), model.getValue(key));
    assertFalse(model.isModified());
    assertFalse(model.isModified(key));

    model.setValue(key, 2);
    assertTrue(model.containsValue(key));
    assertEquals(Integer.valueOf(2), model.getValue(key));
    assertEquals(Integer.valueOf(1), model.getOriginalValue(key));
    assertTrue(model.isModified());
    assertTrue(model.isModified(key));

    model.setValue(key, 3);
    assertTrue(model.containsValue(key));
    assertEquals(Integer.valueOf(3), model.getValue(key));
    assertEquals(Integer.valueOf(1), model.getOriginalValue(key));
    assertTrue(model.isModified());
    assertTrue(model.isModified(key));

    model.revertAll();
    assertEquals(Integer.valueOf(1), model.getValue(key));
    assertFalse(model.isModified());
    assertFalse(model.isModified(key));

    model.setValue(key, null);
    assertTrue(model.isValueNull(key));
    assertTrue(model.isModified());
    assertTrue(model.isModified(key));

    model.removeValue(key);
    assertFalse(model.containsValue(key));
    assertFalse(model.isModified());
  }

  @Test
  public void equals() {
    final ValueChangeMap<String, Integer> mapOne = new ValueChangeMapImpl<String, Integer>();
    final ValueChangeMap<String, Integer> mapTwo = (ValueChangeMap) mapOne.getInstance();
    mapOne.eventValueChanged();
    mapTwo.eventValueChanged();

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
}
