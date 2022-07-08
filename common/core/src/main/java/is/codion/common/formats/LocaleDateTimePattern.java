/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.formats;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Specifies a locale sensitive numerical date format pattern.
 * Note that the time part is 24 hour based and is not locale sensitive.
 *
 * Orders the year and month parts according to locale,
 * with two-digit month and day parts and two or four digit year.
 *
 * <pre>
 *
 * LocaleDateTimePattern pattern = LocaleDateTimePattern.builder()
 *     .delimiterDash()
 *     .yearFourDigits()
 *     .hoursMinutes()
 *     .build();
 *
 * Locale iceland = new Locale("is", "IS");
 * Locale us = new Locale("en", "US");
 *
 * pattern.getDatePattern(iceland);    // "dd-MM-yyyy"
 * pattern.getDatePattern(us);         // "MM-dd-yyyy"
 *
 * pattern.getDateTimePattern(iceland);// "dd-MM-yyyy HH:mm"
 * pattern.getDateTimePattern(us)     ;// "MM-dd-yyyy HH:mm"
 * </pre>
 * @see #builder()
 */
public interface LocaleDateTimePattern {

  /**
   * @return the time part of this format, null if none is available
   */
  String getTimePattern();

  /**
   * @return the date part of this format using the default Locale
   */
  String getDatePattern();

  /**
   * @return the date and time (if available) parts of this format using the default Locale
   */
  String getDateTimePattern();

  /**
   * @param locale the locale
   * @return the date part of this format
   */
  String getDatePattern(Locale locale);

  /**
   * @param locale the locale
   * @return the date and time (if available) parts of this format
   */
  String getDateTimePattern(Locale locale);

  /**
   * @return a new {@link DateTimeFormatter} instance based on this pattern
   */
  DateTimeFormatter createFormatter();

  /**
   * @return a new Builder for a {@link LocaleDateTimePattern}.
   */
  static Builder builder() {
    return new DefaultLocaleDateTimePattern.DefaultBuilder();
  }

  /**
   * A Builder for {@link LocaleDateTimePattern}.
   */
  interface Builder {

    /**
     * @param delimiter the date delimiter
     * @return this Builder instance
     */
    Builder delimiter(String delimiter);

    /**
     * @return this Builder instance
     */
    Builder delimiterDash();

    /**
     * @return this Builder instance
     */
    Builder delimiterDot();

    /**
     * @return this Builder instance
     */
    Builder delimiterSlash();

    /**
     * @return this Builder instance
     */
    Builder yearTwoDigits();

    /**
     * @return this Builder instance
     */
    Builder yearFourDigits();

    /**
     * @return this Builder instance
     */
    Builder hoursMinutes();

    /**
     * @return this Builder instance
     */
    Builder hoursMinutesSeconds();

    /**
     * @return this Builder instance
     */
    Builder hoursMinutesSecondsMilliseconds();

    /**
     * @return a new {@link LocaleDateTimePattern} based on this builder.
     */
    LocaleDateTimePattern build();
  }
}
