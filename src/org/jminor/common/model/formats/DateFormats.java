/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.formats;

import java.text.SimpleDateFormat;

/**
 * A collection of date format strings.
 */
public class DateFormats {

  private DateFormats() {}

  /** dd-MM-yy */
  public static final String COMPACT_DASH = "dd-MM-yy";
  /** ddMMyy */
  public static final String COMPACT = "ddMMyy";
  /** dd.MM.yy */
  public static final String COMPACT_DOT = "dd.MM.yy";
  /** dd-MM-yyyy */
  public static final String SHORT_DASH = "dd-MM-yyyy";
  /** dd.MM.yyyy */
  public static final String SHORT_DOT = "dd.MM.yyyy";
  /** HH:mm */
  public static final String SHORT_TIME = "HH:mm";
  /** dd-MM-yyyy HH:mm */
  public static final String TIMESTAMP = "dd-MM-yyyy HH:mm";
  /** ddMMyy HH:mm */
  public static final String COMPACT_TIMESTAMP = "ddMMyy HH:mm";
  /** dd-MM-yy HH:mm */
  public static final String SHORT_TIMESTAMP = "dd-MM-yy HH:mm";
  /** dd-MM-yyyy HH:mm:ss.SSS */
  public static final String EXACT_TIMESTAMP = "dd-MM-yyyy HH:mm:ss.SSS";
  /** dd-MM-yyyy HH:mm:ss */
  public static final String FULL_TIMESTAMP = "dd-MM-yyyy HH:mm:ss";

  /**
   * Instantiates a non-lenient date format
   * @param formatString the format string
   * @return a non-lenient date format
   */
  public static SimpleDateFormat getDateFormat(final String formatString) {
    return getDateFormat(formatString, false);
  }

  /**
   * Instantiates a date format
   * @param formatString the format string
   * @param lenient the lenient status
   * @return a date format
   */
  public static SimpleDateFormat getDateFormat(final String formatString, final boolean lenient) {
    final SimpleDateFormat format = new SimpleDateFormat(formatString);
    format.setLenient(lenient);

    return format;
  }
}
