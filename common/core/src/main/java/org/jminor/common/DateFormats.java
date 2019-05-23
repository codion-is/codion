/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * A collection of date format strings.
 */
public final class DateFormats {

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
  public static final String EXACT_TIMESTAMP = "yyyy-MM-dd HH:mm:ss.SSS";
  /** dd-MM-yyyy HH:mm:ss */
  public static final String FULL_TIMESTAMP = "dd-MM-yyyy HH:mm:ss";
  /** ddMMyy HH:mm:ss */
  public static final String FULL_COMPACT_TIMESTAMP = "ddMMyy HH:mm:ss";

  private DateFormats() {}

  /**
   * Parses the date pattern and returns mask string that can be used in JFormattedFields.
   * This only works with plain numerical date formats.
   * @param dateFormat the format pattern from which to retrieve the date mask
   * @return a String representing the mask to use in JFormattedTextFields, i.e. "##-##-####"
   */
  public static String getDateMask(final String dateFormat) {
    final StringBuilder stringBuilder = new StringBuilder(dateFormat.length());
    for (final Character character : dateFormat.toCharArray()) {
      stringBuilder.append(Character.isLetter(character) ? "#" : character);
    }

    return stringBuilder.toString();
  }

  /**
   * Parses a Temporal value from text with a provided formatter
   * @param <T> the Temporal type
   */
  public interface DateParser<T> {
    /**
     * Parses the given text with the given formatter
     * @param text the text to parse
     * @param formatter the formatter to use
     * @return the Temporal value
     * @throws DateTimeParseException if unable to parse the text
     */
    T parse(final String text, final DateTimeFormatter formatter) throws DateTimeParseException;
  }
}
