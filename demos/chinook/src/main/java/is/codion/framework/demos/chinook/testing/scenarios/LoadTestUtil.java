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
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.chinook.testing.scenarios;

import java.util.Random;

final class LoadTestUtil {

  private static final int MAX_ARTIST_ID = 275;
  private static final int MAX_CUSTOMER_ID = 59;

  static final Random RANDOM = new Random();

  static long randomArtistId() {
    return RANDOM.nextInt(MAX_ARTIST_ID) + 1;
  }

  static long randomCustomerId() {
    return RANDOM.nextInt(MAX_CUSTOMER_ID) + 1;
  }
}
