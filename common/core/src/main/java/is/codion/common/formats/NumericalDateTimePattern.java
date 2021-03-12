/*
 * Copyright (c) 2021 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.formats;

import java.util.Locale;

import static java.util.Objects.requireNonNull;

/**
 * Specifies a locale sensitive numerical date/time format pattern.
 * Note that the time part is 24 hour based.
 *
 * Orders the year and month parts according to locale,
 * with two digit month and day parts and two or four digit year.
 * @see #builder()
 */
public interface NumericalDateTimePattern {

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
   * Parses the date pattern and returns mask string that can be used in JFormattedFields.
   * This only works with plain numerical date formats.
   * @param numericalDatePattern the format pattern for which to create the mask
   * @return a String representing the mask to use in JFormattedTextFields, i.e. "##-##-####"
   */
  static String getMask(final String numericalDatePattern) {
    requireNonNull(numericalDatePattern, "dateFormat");
    final StringBuilder stringBuilder = new StringBuilder(numericalDatePattern.length());
    for (final Character character : numericalDatePattern.toCharArray()) {
      stringBuilder.append(Character.isLetter(character) ? "#" : character);
    }

    return stringBuilder.toString();
  }

  /**
   * @return a new Builder for a {@link NumericalDateTimePattern}.
   */
  static Builder builder() {
    return new DefaultNumericalDatePattern.DefaultBuilder();
  }

  /**
   * A Builder for {@link NumericalDateTimePattern}.
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
    Builder twoDigitYear();

    /**
     * @return this Builder instance
     */
    Builder fourDigitYear();

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
     * @return a new {@link NumericalDateTimePattern} based on this builder.
     */
    NumericalDateTimePattern build();
  }
}
