/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

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
    return getNonGroupingNumberFormat(false);
  }

  /**
   * @param integerFormat if true an integer based number format is returned
   * @return a NumberFormat instance with grouping disabled
   */
  public static NumberFormat getNonGroupingNumberFormat(final boolean integerFormat) {
    final NumberFormat format = integerFormat ? NumberFormat.getIntegerInstance() : NumberFormat.getNumberInstance();
    format.setGroupingUsed(false);

    return format;
  }

  /**
   * A simple null format, which performs no formatting
   */
  private static final class NullFormat extends Format {

    private static final long serialVersionUID = 1;

    @Override
    public StringBuffer format(final Object obj, final StringBuffer toAppendTo, final FieldPosition pos) {
      toAppendTo.append(obj.toString());
      return toAppendTo;
    }

    @Override
    public Object parseObject(final String source, final ParsePosition pos) {
      pos.setIndex(source.length());
      return source;
    }
  }
}