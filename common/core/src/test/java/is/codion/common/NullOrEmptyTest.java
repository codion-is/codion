/*
 * Copyright (c) 2016 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class NullOrEmptyTest {

  @Test
  void notNull() {
    assertTrue(NullOrEmpty.notNull(new Object(), new Object(), new Object()));
    assertTrue(NullOrEmpty.notNull(new Object()));
    assertFalse(NullOrEmpty.notNull(new Object(), null, new Object()));
    Object ob = null;
    assertFalse(NullOrEmpty.notNull(ob));
    assertFalse(NullOrEmpty.notNull((Object[]) null));
  }

  @Test
  void nullOrEmpty() {
    Map<Integer, String> map = new HashMap<>();
    map.put(1, "1");

    assertTrue(NullOrEmpty.nullOrEmpty((String[]) null));
    assertTrue(NullOrEmpty.nullOrEmpty("sadf", null));
    assertTrue(NullOrEmpty.nullOrEmpty("asdf", ""));

    assertFalse(NullOrEmpty.nullOrEmpty(singletonList("1")));
    assertFalse(NullOrEmpty.nullOrEmpty(asList("1", "2")));

    assertFalse(NullOrEmpty.nullOrEmpty("asdf"));
    assertFalse(NullOrEmpty.nullOrEmpty("asdf", "wefs"));

    assertFalse(NullOrEmpty.nullOrEmpty(map));
  }
}
