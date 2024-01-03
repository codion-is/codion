/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2016 - 2024, Björn Darri Sigurðsson.
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
