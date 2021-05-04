/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import is.codion.common.DateTimeParser;
import is.codion.common.formats.LocaleDateTimePattern;
import is.codion.swing.common.ui.textfield.TextFields.FieldFormatter;
import is.codion.swing.common.ui.textfield.TextFields.ValueContainsLiterals;

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
 * A JFormattedTextField for Temporal types.
 * @param <T> the temporal type
 */
public final class TemporalField<T extends Temporal> extends JFormattedTextField {

  private final Class<T> temporalClass;
  private final String dateTimePattern;
  private final DateTimeFormatter formatter;
  private final DateTimeParser<T> dateTimeParser;

  /**
   * Instantiates a new {@link TemporalField}.
   * @param temporalClass the Temporal type class
   * @param dateTimePattern the date/time pattern
   */
  public TemporalField(final Class<T> temporalClass, final String dateTimePattern) {
    super(initializeFormatter(requireNonNull(dateTimePattern, "dateTimePattern")));
    this.temporalClass = requireNonNull(temporalClass, "temporalClass");
    this.dateTimePattern = dateTimePattern;
    this.formatter = DateTimeFormatter.ofPattern(dateTimePattern);
    this.dateTimeParser = initializeDateTimeParser(temporalClass);
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

  private DateTimeParser<T> initializeDateTimeParser(final Class<T> typeClass) {
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
      return new FieldFormatter(LocaleDateTimePattern.getMask(dateTimePattern), ValueContainsLiterals.YES);
    }
    catch (final ParseException e) {
      throw new RuntimeException(e);
    }
  }
}
