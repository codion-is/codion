/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public final class RounderTest {

  @Test
  void roundDouble() {
    final double d = 5.1234567;
    assertNull(Rounder.roundDouble(null, 3));
    assertEquals(Double.valueOf(5.1), Double.valueOf(Rounder.roundDouble(d, 1)));
    assertEquals(Double.valueOf(5.12), Double.valueOf(Rounder.roundDouble(d, 2)));
    assertEquals(Double.valueOf(5.123), Double.valueOf(Rounder.roundDouble(d, 3)));
    assertEquals(Double.valueOf(5.1235), Double.valueOf(Rounder.roundDouble(d, 4)));
    assertEquals(Double.valueOf(5.12346), Double.valueOf(Rounder.roundDouble(d, 5)));
    assertEquals(Double.valueOf(5.123457), Double.valueOf(Rounder.roundDouble(d, 6)));
    assertEquals(Double.valueOf(5.1234567), Double.valueOf(Rounder.roundDouble(d, 7)));
  }
}
