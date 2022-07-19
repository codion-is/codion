/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public final class PrimitivesTest {

  @Test
  void primitives() {
    assertEquals(0d, Primitives.getDefaultValue(Double.TYPE));
    assertEquals(0, Primitives.getDefaultValue(Integer.TYPE));
    assertFalse(Primitives.getDefaultValue(Boolean.TYPE));

    assertEquals(Double.class, Primitives.getBoxedType(Double.TYPE));
    assertEquals(Integer.class, Primitives.getBoxedType(Integer.TYPE));
    assertEquals(Boolean.class, Primitives.getBoxedType(Boolean.TYPE));

    assertThrows(IllegalArgumentException.class, () -> Primitives.getDefaultValue(Double.class));
    assertThrows(IllegalArgumentException.class, () -> Primitives.getDefaultValue(Integer.class));
    assertThrows(IllegalArgumentException.class, () -> Primitives.getDefaultValue(Boolean.class));
  }
}
