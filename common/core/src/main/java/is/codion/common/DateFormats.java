/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * A collection of locale sensitive numerical date format strings.
 * Orders the year and month parts according to the default locale,
 * with two digit month and day parts and two or four digit year.
 */
public final class DateFormats {

  /**
   * Two letter year, dash delimiter, i.e. dd-MM-yy
   */
  public static final String DATE_COMPACT_DASH = getDateFormat(Locale.getDefault(), DateDelimiter.DASH, YearLength.TWO);

  /**
   * Two letter year, no delimiter, i.e. ddMMyy
   */
  public static final String DATE_COMPACT = getDateFormat(Locale.getDefault(), DateDelimiter.NONE, YearLength.TWO);

  /**
   * Two letter year, dot delimiter, i.e. dd.MM.yy
   */
  public static final String DATE_COMPACT_DOT = getDateFormat(Locale.getDefault(), DateDelimiter.DOT, YearLength.TWO);

  /**
   * Four letter year, dash delimiter, i.e. dd-MM-yyyy
   */
  public static final String DATE_SHORT_DASH = getDateFormat(Locale.getDefault(), DateDelimiter.DASH, YearLength.FOUR);

  /**
   * Four letter year, dot delimiter, i.e. dd.MM.yyyy
   */
  public static final String DATE_SHORT_DOT = getDateFormat(Locale.getDefault(), DateDelimiter.DOT, YearLength.FOUR);

  /**
   * HH:mm
   */
  public static final String TIME_SHORT = "HH:mm";

  /**
   * Four letter year, dash delimiter, hours, minutes, i.e. dd-MM-yyyy HH:mm
   */
  public static final String DATE_TIME = getDateTimeFormat(Locale.getDefault(), DateDelimiter.DASH, YearLength.FOUR, TIME_SHORT);

  /**
   * Two letter year, no delimiter, hours, minutes, i.e. ddMMyy HH:mm
   */
  public static final String DATE_TIME_COMPACT = getDateTimeFormat(Locale.getDefault(), DateDelimiter.DASH, YearLength.TWO, TIME_SHORT);

  /**
   * Two letter year, dash delimiter, hours, minutes, i.e. dd-MM-yy HH:mm
   */
  public static final String DATE_TIME_SHORT = getDateTimeFormat(Locale.getDefault(), DateDelimiter.DASH, YearLength.TWO, TIME_SHORT);

  /**
   * Four letter year, dash delimiter, hours, minutes, i.e. dd-MM-yyyy HH:mm
   */
  public static final String DATE_TIME_MEDIUM = getDateTimeFormat(Locale.getDefault(), DateDelimiter.DASH, YearLength.FOUR, TIME_SHORT);

  /**
   * Four letter year, dash delimiter, hours, minutes, seconds, i.e. dd-MM-yyyy HH:mm:ss
   */
  public static final String DATE_TIME_FULL = getDateTimeFormat(Locale.getDefault(), DateDelimiter.DASH, YearLength.FOUR, TIME_SHORT + ":ss");

  /**
   * Four letter year, dash delimiter, hours, minutes, seconds, milliseconds, i.e. dd-MM-yyyy HH:mm:ss.SSS
   */
  public static final String DATE_TIME_EXACT = getDateTimeFormat(Locale.getDefault(), DateDelimiter.DASH, YearLength.FOUR, TIME_SHORT + ":ss.SSS");

  /**
   * Two letter year, no delimiter, hours, minutes, seconds, i.e. ddMMyy HH:mm:ss
   */
  public static final String DATE_TIME_FULL_COMPACT = getDateTimeFormat(Locale.getDefault(), DateDelimiter.NONE, YearLength.TWO, TIME_SHORT + ":ss");

  private DateFormats() {}

  /**
   * Parses the date pattern and returns mask string that can be used in JFormattedFields.
   * This only works with plain numerical date formats.
   * @param dateFormat the format pattern from which to retrieve the date mask
   * @return a String representing the mask to use in JFormattedTextFields, i.e. "##-##-####"
   */
  public static String getDateMask(final String dateFormat) {
    requireNonNull(dateFormat, "dateFormat");
    final StringBuilder stringBuilder = new StringBuilder(dateFormat.length());
    for (final Character character : dateFormat.toCharArray()) {
      stringBuilder.append(Character.isLetter(character) ? "#" : character);
    }

    return stringBuilder.toString();
  }

  private static String getDateFormat(final Locale locale, final DateDelimiter delimiter, final YearLength yearLength) {
    return getDateTimeFormat(locale, delimiter, yearLength, null);
  }

  private static String getDateTimeFormat(final Locale locale, final DateDelimiter delimiter, final YearLength yearLength,
                                          final String timePattern) {
    final String datePattern = DateTimeFormatterBuilder.
            getLocalizedDateTimePattern(FormatStyle.SHORT, null, IsoChronology.INSTANCE, locale).toLowerCase(locale);
    final List<String> pattern = new ArrayList<>(Arrays.asList(null, null, null));
    pattern.set(indexOf(datePattern, Element.YEAR), yearLength == YearLength.FOUR ? "yyyy" : "yy");
    pattern.set(indexOf(datePattern, Element.MONTH), "MM");
    pattern.set(indexOf(datePattern, Element.DAY), "dd");
    final StringBuilder builder = new StringBuilder(String.join(delimiter.getDelimiter() == null ? "" : String.valueOf(delimiter.getDelimiter()), pattern));
    if (timePattern != null) {
      builder.append(" ").append(timePattern);
    }

    return builder.toString();
  }

  private static int indexOf(final String pattern, final Element element) {
    return Stream.of(pattern.indexOf('y'), pattern.indexOf('m'), pattern.indexOf('d'))
            .sorted().collect(Collectors.toList()).indexOf(pattern.indexOf(element.getChar()));
  }

  private enum DateDelimiter {
    DASH {
      @Override
      Character getDelimiter() {
        return '-';
      }
    },
    DOT {
      @Override
      Character getDelimiter() {
        return '.';
      }
    },
    NONE {
      @Override
      Character getDelimiter() {
        return null;
      }
    };

    abstract Character getDelimiter();
  }

  private enum YearLength {
    TWO, FOUR
  }

  private enum Element {
    YEAR {
      @Override
      char getChar() {
        return 'y';
      }
    },
    MONTH {
      @Override
      char getChar() {
        return 'm';
      }
    },
    DAY {
      @Override
      char getChar() {
        return 'd';
      }
    };

    abstract char getChar();
  }
}
