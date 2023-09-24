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
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public final class PrimitivesTest {

  @Test
  void primitives() {
    assertEquals(0d, Primitives.defaultValue(Double.TYPE));
    assertEquals(0, Primitives.defaultValue(Integer.TYPE));
    assertFalse(Primitives.defaultValue(Boolean.TYPE));

    assertEquals(Double.class, Primitives.boxedType(Double.TYPE));
    assertEquals(Integer.class, Primitives.boxedType(Integer.TYPE));
    assertEquals(Boolean.class, Primitives.boxedType(Boolean.TYPE));

    assertThrows(IllegalArgumentException.class, () -> Primitives.defaultValue(Double.class));
    assertThrows(IllegalArgumentException.class, () -> Primitives.defaultValue(Integer.class));
    assertThrows(IllegalArgumentException.class, () -> Primitives.defaultValue(Boolean.class));

    assertThrows(IllegalArgumentException.class, () -> Primitives.boxedType(Number.class));
  }
}
