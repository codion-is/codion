/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
    return d == null ? null : new BigDecimal(Double.toString(d)).setScale(places, roundingMode).doubleValue();
  }
}
