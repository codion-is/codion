/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public final class UtilTest {

  @Test
  public void serializeDeserialize() throws IOException, ClassNotFoundException {
    assertNull(Util.deserialize(new byte[0]));
    assertEquals(0, Util.serialize(null).length);
    assertEquals(Integer.valueOf(4), Util.deserialize(Util.serialize(4)));
  }

  @Test
  public void roundDouble() {
    final double d = 5.1234567;
    assertEquals(Double.valueOf(5.1), Double.valueOf(Util.roundDouble(d, 1)));
    assertEquals(Double.valueOf(5.12), Double.valueOf(Util.roundDouble(d, 2)));
    assertEquals(Double.valueOf(5.123), Double.valueOf(Util.roundDouble(d, 3)));
    assertEquals(Double.valueOf(5.1235), Double.valueOf(Util.roundDouble(d, 4)));
    assertEquals(Double.valueOf(5.12346), Double.valueOf(Util.roundDouble(d, 5)));
    assertEquals(Double.valueOf(5.123457), Double.valueOf(Util.roundDouble(d, 6)));
    assertEquals(Double.valueOf(5.1234567), Double.valueOf(Util.roundDouble(d, 7)));
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

  @Test
  public void getGetMethodInvalidMethod() throws NoSuchMethodException {
    final Bean bean = new Bean();
    assertThrows(NoSuchMethodException.class, () -> Util.getGetMethod(boolean.class, "invalidValue", bean));
  }

  @Test
  public void getSetMethodInvalidMethod() throws NoSuchMethodException {
    final Bean bean = new Bean();
    assertThrows(NoSuchMethodException.class, () -> Util.getSetMethod(boolean.class, "invalidValue", bean));
  }

  @Test
  public void getSetMethodNoProperty() throws NoSuchMethodException {
    final Bean bean = new Bean();
    assertThrows(IllegalArgumentException.class, () -> Util.getSetMethod(boolean.class, "", bean));
  }

  @Test
  public void getGetMethodNoProperty() throws NoSuchMethodException {
    final Bean bean = new Bean();
    assertThrows(IllegalArgumentException.class, () -> Util.getGetMethod(boolean.class, "", bean));
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
