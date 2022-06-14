/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public final class PrimitiveTypesTest {

  @Test
  void primitives() {
    assertEquals(0d, PrimitiveTypes.getDefaultValue(Double.TYPE));
    assertEquals(0, PrimitiveTypes.getDefaultValue(Integer.TYPE));
    assertFalse(PrimitiveTypes.getDefaultValue(Boolean.TYPE));

    assertEquals(Double.class, PrimitiveTypes.getBoxedType(Double.TYPE));
    assertEquals(Integer.class, PrimitiveTypes.getBoxedType(Integer.TYPE));
    assertEquals(Boolean.class, PrimitiveTypes.getBoxedType(Boolean.TYPE));

    assertThrows(IllegalArgumentException.class, () -> PrimitiveTypes.getDefaultValue(Double.class));
    assertThrows(IllegalArgumentException.class, () -> PrimitiveTypes.getDefaultValue(Integer.class));
    assertThrows(IllegalArgumentException.class, () -> PrimitiveTypes.getDefaultValue(Boolean.class));
  }
}
