/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class UtilTest {

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
    assertTrue(Util.nullOrEmpty(singletonList(""), null));
    assertTrue(Util.nullOrEmpty(singletonList(""), emptyList()));

    final Map<Integer, String> map = new HashMap<>();
    map.put(1, "1");
    assertTrue(Util.nullOrEmpty((Map[]) null));
    assertTrue(Util.nullOrEmpty(map, null));
    assertTrue(Util.nullOrEmpty(map, Collections.emptyMap()));

    assertTrue(Util.nullOrEmpty((String[]) null));
    assertTrue(Util.nullOrEmpty("sadf", null));
    assertTrue(Util.nullOrEmpty("asdf", ""));

    assertFalse(Util.nullOrEmpty(singletonList("1")));
    assertFalse(Util.nullOrEmpty(asList("1", "2")));

    assertFalse(Util.nullOrEmpty("asdf"));
    assertFalse(Util.nullOrEmpty("asdf", "wefs"));

    assertFalse(Util.nullOrEmpty(map));
  }
}
