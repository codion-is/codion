/*
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
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
