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

import java.math.BigDecimal;
import java.math.RoundingMode;

import static java.util.Objects.requireNonNull;

/**
 * A utility class for rounding doubles.
 */
public final class Rounder {

  private Rounder() {}

  /**
   * Rounds the given double to {@code places} decimal places, using {@link RoundingMode#HALF_UP}.
   * @param d the double to round, null results in a null return value
   * @param places the number of decimal places
   * @return the rounded value or null if the parameter value was null
   */
  public static Double roundDouble(Double d, int places) {
    return roundDouble(d, places, RoundingMode.HALF_UP);
  }

  /**
   * Rounds the given double to {@code places} decimal places.
   * @param d the double to round, null results in a null return value
   * @param places the number of decimal places
   * @param roundingMode the rounding mode
   * @return the rounded value or null if the parameter value was null
   */
  public static Double roundDouble(Double d, int places, RoundingMode roundingMode) {
    return d == null ? null : new BigDecimal(Double.toString(d)).setScale(places, requireNonNull(roundingMode)).doubleValue();
  }
}
