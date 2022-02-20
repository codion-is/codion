/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class UtilTest {

  @Test
  void roundDouble() {
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
  void notNull() throws Exception {
    assertTrue(Util.notNull(new Object(), new Object(), new Object()));
    assertTrue(Util.notNull(new Object()));
    assertFalse(Util.notNull(new Object(), null, new Object()));
    final Object ob = null;
    assertFalse(Util.notNull(ob));
    assertFalse(Util.notNull((Object[]) null));
  }

  @Test
  void onClasspath() {
    assertTrue(Util.onClasspath(UtilTest.class.getName()));
    assertFalse(Util.onClasspath("no.class.Here"));
  }

  @Test
  void nullOrEmpty() {
    final Map<Integer, String> map = new HashMap<>();
    map.put(1, "1");

    assertTrue(Util.nullOrEmpty((String[]) null));
    assertTrue(Util.nullOrEmpty("sadf", null));
    assertTrue(Util.nullOrEmpty("asdf", ""));

    assertFalse(Util.nullOrEmpty(singletonList("1")));
    assertFalse(Util.nullOrEmpty(asList("1", "2")));

    assertFalse(Util.nullOrEmpty("asdf"));
    assertFalse(Util.nullOrEmpty("asdf", "wefs"));

    assertFalse(Util.nullOrEmpty(map));
  }

  @Test
  void primitives() {
    assertEquals(0d, Util.getPrimitiveDefaultValue(Double.TYPE));
    assertEquals(0, Util.getPrimitiveDefaultValue(Integer.TYPE));

    assertEquals(Double.class, Util.getPrimitiveBoxedType(Double.TYPE));
    assertEquals(Integer.class, Util.getPrimitiveBoxedType(Integer.TYPE));

    assertThrows(IllegalArgumentException.class, () -> Util.getPrimitiveDefaultValue(Double.class));
    assertThrows(IllegalArgumentException.class, () -> Util.getPrimitiveDefaultValue(Integer.class));
  }
}
