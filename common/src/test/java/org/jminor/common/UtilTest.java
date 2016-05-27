/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import org.junit.Test;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public final class UtilTest {

  @Test
  public void roundDouble() {
    final Double d = 5.1234567;
    assertEquals(new Double(5.1), new Double(Util.roundDouble(d, 1)));
    assertEquals(new Double(5.12), new Double(Util.roundDouble(d, 2)));
    assertEquals(new Double(5.123), new Double(Util.roundDouble(d, 3)));
    assertEquals(new Double(5.1235), new Double(Util.roundDouble(d, 4)));
    assertEquals(new Double(5.12346), new Double(Util.roundDouble(d, 5)));
    assertEquals(new Double(5.123457), new Double(Util.roundDouble(d, 6)));
    assertEquals(new Double(5.1234567), new Double(Util.roundDouble(d, 7)));
  }

  @Test
  public void closeSilently() {
    Util.closeSilently((Closeable) null);
    Util.closeSilently((Closeable[]) null);
    Util.closeSilently(null, null);
  }

  @Test
  public void getGetMethod() throws NoSuchMethodException {
    final Bean bean = new Bean();
    Method getMethod = Util.getGetMethod(boolean.class, "booleanValue", bean);
    assertEquals("isBooleanValue", getMethod.getName());
    getMethod = Util.getGetMethod(int.class, "intValue", bean);
    assertEquals("getIntValue", getMethod.getName());
  }

  @Test
  public void getGetMethodBoolean() throws NoSuchMethodException {
    final Bean bean = new Bean();
    final Method getMethod = Util.getGetMethod(boolean.class, "anotherBooleanValue", bean);
    assertEquals("getAnotherBooleanValue", getMethod.getName());
  }

  @Test
  public void getSetMethod() throws NoSuchMethodException {
    final Bean bean = new Bean();
    Method setMethod = Util.getSetMethod(boolean.class, "booleanValue", bean);
    assertEquals("setBooleanValue", setMethod.getName());
    setMethod = Util.getSetMethod(int.class, "intValue", bean);
    assertEquals("setIntValue", setMethod.getName());
  }

  @Test(expected = NoSuchMethodException.class)
  public void getGetMethodInvalidMethod() throws NoSuchMethodException {
    final Bean bean = new Bean();
    Util.getGetMethod(boolean.class, "invalidValue", bean);
  }

  @Test(expected = NoSuchMethodException.class)
  public void getSetMethodInvalidMethod() throws NoSuchMethodException {
    final Bean bean = new Bean();
    Util.getSetMethod(boolean.class, "invalidValue", bean);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getSetMethodNoProperty() throws NoSuchMethodException {
    final Bean bean = new Bean();
    Util.getSetMethod(boolean.class, "", bean);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getGetMethodNoProperty() throws NoSuchMethodException {
    final Bean bean = new Bean();
    Util.getGetMethod(boolean.class, "", bean);
  }

  @Test
  public void getArrayContentsAsString() throws Exception {
    assertEquals("", Util.getArrayContentsAsString(null, true));
    String res = Util.getArrayContentsAsString(new Object[] {1, 2,new Object[] {3, 4}}, false);
    assertEquals("Integer array as string should work", "1, 2, 3, 4", res);
    res = Util.getArrayContentsAsString(new Object[] {1, 2,new Object[] {3, 4}}, true);
    assertEquals("Integer array as string should work", "1\n2\n3\n4\n", res);
  }

  @Test
  public void getListContentsAsString() throws Exception {
    final List<Integer> list = new ArrayList<>();
    list.add(1);
    list.add(2);
    list.add(3);
    list.add(4);
    final String res = Util.getCollectionContentsAsString(list, false);
    assertEquals("Integer list as string should work", "1, 2, 3, 4", res);
  }

  @Test
  public void notNull() throws Exception {
    assertTrue(Util.notNull(new Object(), new Object(), new Object()));
    assertTrue(Util.notNull(new Object()));
    assertFalse(Util.notNull(new Object(), null, new Object()));
    final Object ob = null;
    assertFalse(Util.notNull(ob));
    assertFalse(Util.notNull((Object[]) null));
  }

  @Test
  public void onClasspath() {
    assertTrue(Util.onClasspath(UtilTest.class.getName()));
    assertFalse(Util.onClasspath("no.class.Here"));
  }

  @Test
  public void nullOrEmpty() {
    assertTrue(Util.nullOrEmpty((Collection[]) null));
    assertTrue(Util.nullOrEmpty(Collections.singletonList(""), null));
    assertTrue(Util.nullOrEmpty(Collections.singletonList(""), Collections.emptyList()));

    final Map<Integer, String> map = new HashMap<>();
    map.put(1, "1");
    assertTrue(Util.nullOrEmpty((Map[]) null));
    assertTrue(Util.nullOrEmpty(map, null));
    assertTrue(Util.nullOrEmpty(map, Collections.emptyMap()));

    assertTrue(Util.nullOrEmpty((String[]) null));
    assertTrue(Util.nullOrEmpty("sadf", null));
    assertTrue(Util.nullOrEmpty("asdf", ""));

    assertFalse(Util.nullOrEmpty(Collections.singletonList("1")));
    assertFalse(Util.nullOrEmpty(Arrays.asList("1", "2")));

    assertFalse(Util.nullOrEmpty("asdf"));
    assertFalse(Util.nullOrEmpty("asdf", "wefs"));

    assertFalse(Util.nullOrEmpty(map));
  }

  public static final class Bean {
    boolean booleanValue;
    boolean anotherBooleanValue;
    int intValue;

    public boolean isBooleanValue() {
      return booleanValue;
    }

    public void setBooleanValue(final boolean booleanValue) {
      this.booleanValue = booleanValue;
    }

    public boolean getAnotherBooleanValue() {
      return anotherBooleanValue;
    }

    public int getIntValue() {
      return intValue;
    }

    public void setIntValue(final int intValue) {
      this.intValue = intValue;
    }
  }
}
