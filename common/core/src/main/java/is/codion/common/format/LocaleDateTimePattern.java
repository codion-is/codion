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
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.common.format;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Specifies a locale sensitive numerical date format pattern.
 * Note that the time part is 24 hour based and is not locale sensitive.
 * <br>
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
 * pattern.datePattern(iceland);    // "dd-MM-yyyy"
 * pattern.datePattern(us);         // "MM-dd-yyyy"
 *
 * pattern.dateTimePattern(iceland);// "dd-MM-yyyy HH:mm"
 * pattern.dateTimePattern(us)     ;// "MM-dd-yyyy HH:mm"
 * </pre>
 * @see #builder()
 */
public interface LocaleDateTimePattern {

  /**
   * @return the time part of this format, null if none is available
   */
  String timePattern();

  /**
   * @return the date part of this format using the default Locale
   */
  String datePattern();

  /**
   * @return the date and time (if available) parts of this format using the default Locale
   */
  String dateTimePattern();

  /**
   * @param locale the locale
   * @return the date part of this format
   */
  String datePattern(Locale locale);

  /**
   * @param locale the locale
   * @return the date and time (if available) parts of this format
   */
  String dateTimePattern(Locale locale);

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
