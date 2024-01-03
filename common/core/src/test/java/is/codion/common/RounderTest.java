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
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson.
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
