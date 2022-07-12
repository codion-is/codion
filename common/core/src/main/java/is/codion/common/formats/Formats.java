/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.formats;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * A static utility class for formats.
 */
public final class Formats {

  private Formats() {}

  /**
   * @return a NumberFormat instance with grouping disabled
   */
  public static NumberFormat getNonGroupingNumberFormat() {
    NumberFormat format = NumberFormat.getNumberInstance();
    format.setGroupingUsed(false);

    return format;
  }

  /**
   * @return an Integer NumberFormat instance with grouping disabled
   */
  public static NumberFormat getNonGroupingIntegerFormat() {
    NumberFormat format = NumberFormat.getIntegerInstance();
    format.setGroupingUsed(false);

    return format;
  }

  /**
   * @return a BigDecimal parsing DecimalFormat
   */
  public static DecimalFormat getBigDecimalNumberFormat() {
    DecimalFormat format = (DecimalFormat) NumberFormat.getNumberInstance();
    format.setParseBigDecimal(true);

    return format;
  }
}