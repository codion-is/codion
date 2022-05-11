/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.formats;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParsePosition;

/**
 * A static utility class for formats.
 */
public final class Formats {

  /**
   * A Format object performing no formatting
   */
  public static final Format NULL_FORMAT = new NullFormat();

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

  /**
   * A null format, which performs no formatting
   */
  private static final class NullFormat extends Format {

    private static final long serialVersionUID = 1;

    @Override
    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
      toAppendTo.append(obj == null ? "" : obj.toString());
      return toAppendTo;
    }

    @Override
    public Object parseObject(String source, ParsePosition pos) {
      pos.setIndex(source.length());
      return source;
    }
  }
}