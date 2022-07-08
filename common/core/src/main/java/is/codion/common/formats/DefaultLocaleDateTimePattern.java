/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.formats;

import java.io.Serializable;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

final class DefaultLocaleDateTimePattern implements LocaleDateTimePattern, Serializable {

  private static final long serialVersionUID = 1;

  private static final String FOUR_DIGIT_YEAR = "yyyy";
  private static final String TWO_DIGIT_YEAR = "yy";
  private static final String TWO_DIGIT_MONTH = "MM";
  private static final String TWO_DIGIT_DAY = "dd";
  private static final String HOURS_MINUTES = "HH:mm";
  private static final String HOURS_MINUTES_SECONDS = "HH:mm:ss";
  private static final String HOURS_MINUTES_SECONDS_MILLISECONDS = "HH:mm:ss.SSS";

  private final String delimiter;
  private final boolean fourDigitYear;
  private final String timeFormat;

  private DefaultLocaleDateTimePattern(DefaultBuilder builder) {
    this.delimiter = requireNonNull(builder.delimiter, "delimiter");
    this.fourDigitYear = builder.fourDigitYear;
    this.timeFormat = builder.timeFormat;
  }

  @Override
  public String getTimePattern() {
    return timeFormat;
  }

  @Override
  public String getDatePattern() {
    return getDatePattern(Locale.getDefault());
  }

  @Override
  public String getDateTimePattern() {
    return getDateTimePattern(Locale.getDefault());
  }

  @Override
  public String getDatePattern(Locale locale) {
    return getDatePattern(locale, delimiter, fourDigitYear);
  }

  @Override
  public String getDateTimePattern(Locale locale) {
    return getDateTimePattern(locale, delimiter, fourDigitYear, timeFormat);
  }

  @Override
  public DateTimeFormatter createFormatter() {
    return DateTimeFormatter.ofPattern(getDateTimePattern());
  }

  private static String getDatePattern(Locale locale, String delimiter, boolean fourDigitYear) {
    return getDateTimePattern(locale, delimiter, fourDigitYear, null);
  }

  private static String getDateTimePattern(Locale locale, String delimiter, boolean fourDigitYear,
                                           String timePattern) {
    String datePattern = DateTimeFormatterBuilder.
            getLocalizedDateTimePattern(FormatStyle.SHORT, null, IsoChronology.INSTANCE, locale).toLowerCase(locale);
    List<String> pattern = new ArrayList<>(Arrays.asList(null, null, null));
    pattern.set(indexOf(datePattern, Element.YEAR), fourDigitYear ? FOUR_DIGIT_YEAR : TWO_DIGIT_YEAR);
    pattern.set(indexOf(datePattern, Element.MONTH), TWO_DIGIT_MONTH);
    pattern.set(indexOf(datePattern, Element.DAY), TWO_DIGIT_DAY);
    StringBuilder builder = new StringBuilder(String.join(delimiter, pattern));
    if (timePattern != null) {
      builder.append(" ").append(timePattern);
    }

    return builder.toString();
  }

  private static int indexOf(String pattern, Element element) {
    return Stream.of(pattern.indexOf('y'), pattern.indexOf('m'), pattern.indexOf('d'))
            .sorted().collect(Collectors.toList()).indexOf(pattern.indexOf(element.getChar()));
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

  static final class DefaultBuilder implements Builder {

    private String delimiter = "";
    private boolean fourDigitYear = true;
    private String timeFormat;

    @Override
    public Builder delimiter(String delimiter) {
      this.delimiter = requireNonNull(delimiter);
      return this;
    }

    @Override
    public Builder delimiterDash() {
      return delimiter("-");
    }

    @Override
    public Builder delimiterDot() {
      return delimiter(".");
    }

    @Override
    public Builder delimiterSlash() {
      return delimiter("/");
    }

    @Override
    public Builder yearTwoDigits() {
      this.fourDigitYear = false;
      return this;
    }

    @Override
    public Builder yearFourDigits() {
      this.fourDigitYear = true ;
      return this;
    }

    @Override
    public Builder hoursMinutes() {
      this.timeFormat = HOURS_MINUTES;
      return this;
    }

    @Override
    public Builder hoursMinutesSeconds() {
      this.timeFormat = HOURS_MINUTES_SECONDS;
      return this;
    }

    @Override
    public Builder hoursMinutesSecondsMilliseconds() {
      this.timeFormat = HOURS_MINUTES_SECONDS_MILLISECONDS;
      return this;
    }

    @Override
    public LocaleDateTimePattern build() {
      return new DefaultLocaleDateTimePattern(this);
    }
  }
}
