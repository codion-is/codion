package org.jminor.common.model.valuemap;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.util.Collection;

/**
 * User: Björn Darri
 * Date: 24.7.2010
 * Time: 22:24:49
 */
public class ValueMapImplTest {

  @Test
  public void test() throws Exception {
    final String key = "key";
    final ValueMapImpl<String, Object> map = new ValueMapImpl<String, Object>();
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

    map.setValue(key, key);
    final ValueMap<String, Object> mapCopy = map.getCopy();
    assertEquals(map, mapCopy);
    assertEquals(map.hashCode(), mapCopy.hashCode());

    final String keyTwo = "key2";
    mapCopy.setValue(keyTwo, keyTwo);
    assertEquals(2, mapCopy.size());
    assertFalse(map.equals(mapCopy));

    final Collection<Object> values = mapCopy.getValues();
    assertTrue(values.contains(key));
    assertTrue(values.contains(keyTwo));

    final Collection<String> keys = mapCopy.getValueKeys();
    assertTrue(keys.contains(key));
    assertTrue(keys.contains(keyTwo));

    map.setAs(mapCopy);
    assertEquals(map, mapCopy);
    mapCopy.setValue(keyTwo, "newKey");
    assertFalse(map.equals(mapCopy));

    final ValueMap<String, Object> newMap = map.getInstance();
    assertEquals(0, newMap.size());
  }
}
