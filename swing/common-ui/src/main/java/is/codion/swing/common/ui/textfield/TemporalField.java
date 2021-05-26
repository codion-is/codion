/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import is.codion.common.DateTimeParser;
import is.codion.common.formats.LocaleDateTimePattern;

import javax.swing.JFormattedTextField;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;

import static java.util.Objects.requireNonNull;

/**
 * A JFormattedTextField for Temporal types.<br>
 * @see #localTimeField(String)
 * @see #localDateField(String)
 * @see #localDateTimeField(String)
 * @see #offsetDateTimeField(String)
 * @see #builder(Class)
 * @param <T> the temporal type
 */
public final class TemporalField<T extends Temporal> extends JFormattedTextField {

  private final Class<T> temporalClass;
  private final String dateTimePattern;
  private final DateTimeFormatter formatter;
  private final DateTimeParser<T> dateTimeParser;

  private TemporalField(final Class<T> temporalClass, final String dateTimePattern,
                        final DateTimeParser<T> dateTimeParser) {
    super(initializeFormatter(dateTimePattern));
    this.temporalClass = temporalClass;
    this.dateTimePattern = dateTimePattern;
    this.formatter = DateTimeFormatter.ofPattern(dateTimePattern);
    this.dateTimeParser = requireNonNull(dateTimeParser, "dateTimeParser");
    setFocusLostBehavior(JFormattedTextField.COMMIT);
  }

  /**
   * @return the Temporal class this field is based on
   */
  public Class<T> getTemporalClass() {
    return temporalClass;
  }

  /**
   * @return the Temporal value currently being displayed, null in case of an incomplete date
   * @throws DateTimeParseException if unable to parse the text
   */
  public T getTemporal() throws DateTimeParseException {
    final String text = getText();
    if (!text.contains("_")) {
      return dateTimeParser.parse(text, formatter);
    }

    return null;
  }

  /**
   * Sets the date in the input field, clears the field if {@code date} is null.
   * @param date the date to set
   */
  public void setTemporal(final Temporal date) {
    setText(date == null ? "" : formatter.format(date));
  }

  /**
   * @return the date/time pattern
   */
  public String getDateTimePattern() {
    return dateTimePattern;
  }

  /**
   * @return the date/time formatter
   */
  public DateTimeFormatter getDateTimeFormatter() {
    return formatter;
  }

  /**
   * @return the date/time parser
   */
  public DateTimeParser<T> getDateTimeParser() {
    return dateTimeParser;
  }

  /**
   * A LocalTime based {@link TemporalField}.
   * @param timePattern the time pattern
   * @return a new temporal field
   */
  public static TemporalField<LocalTime> localTimeField(final String timePattern) {
    return builder(LocalTime.class).dateTimePattern(timePattern).build();
  }

  /**
   * A LocalTime based {@link TemporalField}.
   * @param datePattern the date pattern
   * @return a new temporal field
   */
  public static TemporalField<LocalDate> localDateField(final String datePattern) {
    return builder(LocalDate.class).dateTimePattern(datePattern).build();
  }

  /**
   * A LocalTime based {@link TemporalField}.
   * @param dateTimePattern the date time pattern
   * @return a new temporal field
   */
  public static TemporalField<LocalDateTime> localDateTimeField(final String dateTimePattern) {
    return builder(LocalDateTime.class).dateTimePattern(dateTimePattern).build();
  }

  /**
   * A LocalTime based {@link TemporalField}.
   * @param dateTimePattern the date time pattern
   * @return a new temporal field
   */
  public static TemporalField<OffsetDateTime> offsetDateTimeField(final String dateTimePattern) {
    return builder(OffsetDateTime.class).dateTimePattern(dateTimePattern).build();
  }

  /**
   * A builder for {@link TemporalField}.
   * This builder supports: {@link LocalTime}, {@link LocalDate}, {@link LocalDateTime}, {@link OffsetDateTime},<br>
   * for other {@link Temporal} types use {@link Builder#dateTimeParser} to supply a {@link DateTimeParser} instance.
   * @param temporalClass the temporal class
   * @param <T> the temporal type
   * @return a new builder
   */
  public static <T extends Temporal> Builder<T> builder(final Class<T> temporalClass) {
    return new DefaultBuilder<>(requireNonNull(temporalClass));
  }

  /**
   * A builder for {@link TemporalField}.
   * @param <T> the temporal type
   */
  public interface Builder<T extends Temporal> {

    /**
     * @param dateTimePattern the date/time pattern
     * @return this builder instance
     */
    Builder<T> dateTimePattern(String dateTimePattern);

    /**
     * @param dateTimeParser the date/time parser
     * @return this builder instance
     */
    Builder<T> dateTimeParser(DateTimeParser<T> dateTimeParser);

    /**
     * @return a new {@link TemporalField} instance
     */
    TemporalField<T> build();
  }

  private static final class DefaultBuilder<T extends Temporal> implements Builder<T> {

    private final Class<T> temporalClass;

    private String dateTimePattern;
    private DateTimeParser<T> dateTimeParser;

    private DefaultBuilder(final Class<T> temporalClass) {
      this.temporalClass = temporalClass;
      this.dateTimeParser = initializeDateTimeParser(temporalClass);
    }

    @Override
    public Builder<T> dateTimePattern(final String dateTimePattern) {
      this.dateTimePattern = requireNonNull(dateTimePattern);
      return this;
    }

    @Override
    public Builder<T> dateTimeParser(final DateTimeParser<T> dateTimeParser) {
      this.dateTimeParser = requireNonNull(dateTimeParser);
      return this;
    }

    @Override
    public TemporalField<T> build() {
      if (dateTimePattern == null) {
        throw new IllegalStateException("dateTimePattern must be specified");
      }

      return new TemporalField<>(temporalClass, dateTimePattern, dateTimeParser);
    }
  }

  private static <T extends Temporal> DateTimeParser<T> initializeDateTimeParser(final Class<T> typeClass) {
    if (typeClass.equals(LocalTime.class)) {
      return (DateTimeParser<T>) (DateTimeParser<LocalTime>) LocalTime::parse;
    }
    else if (typeClass.equals(LocalDate.class)) {
      return (DateTimeParser<T>) (DateTimeParser<LocalDate>) LocalDate::parse;
    }
    else if (typeClass.equals(LocalDateTime.class)) {
      return (DateTimeParser<T>) (DateTimeParser<LocalDateTime>) LocalDateTime::parse;
    }
    else if (typeClass.equals(OffsetDateTime.class)) {
      return (DateTimeParser<T>) (DateTimeParser<OffsetDateTime>) OffsetDateTime::parse;
    }

    throw new IllegalArgumentException("TemporalField not implemented for: " + typeClass);
  }

  private static FieldFormatter initializeFormatter(final String dateTimePattern) {
    try {
      return new FieldFormatter(LocaleDateTimePattern.getMask(dateTimePattern), true);
    }
    catch (final ParseException e) {
      throw new RuntimeException(e);
    }
  }
}
