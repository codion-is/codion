/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.textfield;

import is.codion.common.DateTimeParser;
import is.codion.common.event.EventDataListener;
import is.codion.common.formats.LocaleDateTimePattern;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.textfield.DocumentAdapter;

import javax.swing.JFormattedTextField;
import javax.swing.text.MaskFormatter;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * A JFormattedTextField for Temporal types.<br>
 * @see #localTimeField(String)
 * @see #localDateField(String)
 * @see #localDateTimeField(String)
 * @see #offsetDateTimeField(String)
 * @see #builder(Class, String)
 * @param <T> the temporal type
 */
public final class TemporalField<T extends Temporal> extends JFormattedTextField {

  private final Class<T> temporalClass;
  private final String dateTimePattern;
  private final DateTimeFormatter formatter;
  private final DateTimeParser<T> dateTimeParser;
  private final Value<T> value = Value.value();

  private TemporalField(DefaultBuilder<T, ?> builder) {
    super(initializeFormatter(builder.dateTimePattern));
    this.temporalClass = builder.temporalClass;
    this.dateTimePattern = builder.dateTimePattern;
    this.formatter = builder.dateTimeFormatter;
    this.dateTimeParser = builder.dateTimeParser;
    setFocusLostBehavior(builder.focusLostBehaviour);
    getDocument().addDocumentListener((DocumentAdapter) e -> value.set(getTemporal()));
  }

  /**
   * @return the Temporal class this field is based on
   */
  public Class<T> getTemporalClass() {
    return temporalClass;
  }

  /**
   * @return the Temporal value currently being displayed, an empty Optional in case of an incomplete/unparseable date
   */
  public Optional<T> getOptional() {
    return Optional.ofNullable(getTemporal());
  }

  /**
   * @return the Temporal value currently being displayed, null in case of an incomplete/unparseable date
   */
  public T getTemporal() {
    try {
      return dateTimeParser.parse(getText(), formatter);
    }
    catch (DateTimeParseException e) {
      return null;
    }
  }

  /**
   * Sets the temporal value in this field, clears the field if {@code temporal} is null.
   * @param temporal the temporal value to set
   */
  public void setTemporal(Temporal temporal) {
    setText(temporal == null ? "" : formatter.format(temporal));
  }

  /**
   * @param listener notified each time the value changes
   */
  public void addTemporalListener(EventDataListener<T> listener) {
    value.addDataListener(listener);
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
  public static TemporalField<LocalTime> localTimeField(String timePattern) {
    return builder(LocalTime.class, timePattern).build();
  }

  /**
   * A LocalTime based {@link TemporalField}.
   * @param datePattern the date pattern
   * @return a new temporal field
   */
  public static TemporalField<LocalDate> localDateField(String datePattern) {
    return builder(LocalDate.class, datePattern).build();
  }

  /**
   * A LocalTime based {@link TemporalField}.
   * @param dateTimePattern the date time pattern
   * @return a new temporal field
   */
  public static TemporalField<LocalDateTime> localDateTimeField(String dateTimePattern) {
    return builder(LocalDateTime.class, dateTimePattern).build();
  }

  /**
   * A LocalTime based {@link TemporalField}.
   * @param dateTimePattern the date time pattern
   * @return a new temporal field
   */
  public static TemporalField<OffsetDateTime> offsetDateTimeField(String dateTimePattern) {
    return builder(OffsetDateTime.class, dateTimePattern).build();
  }

  /**
   * A builder for {@link TemporalField}.
   * This builder supports: {@link LocalTime}, {@link LocalDate}, {@link LocalDateTime}, {@link OffsetDateTime},<br>
   * for other {@link Temporal} types use {@link Builder#dateTimeParser} to supply a {@link DateTimeParser} instance.
   * @param temporalClass the temporal class
   * @param dateTimePattern the date time pattern
   * @param <T> the temporal type
   * @return a new builder
   */
  public static <T extends Temporal, B extends Builder<T, B>> Builder<T, B> builder(Class<T> temporalClass, String dateTimePattern) {
    return new DefaultBuilder<>(temporalClass, dateTimePattern);
  }

  /**
   * A builder for {@link TemporalField}.
   * @param <T> the temporal type
   * @param <B> the builder type
   */
  public interface Builder<T extends Temporal, B extends Builder<T, B>> {

    /**
     * Sets the {@link DateTimeFormatter} for this field, this formatter must
     * be able to parse the date time pattern this field is based on.
     * @param dateTimeFormatter the date/time formatter
     * @return this builder instance
     */
    B dateTimeFormatter(DateTimeFormatter dateTimeFormatter);

    /**
     * @param dateTimeParser the date/time parser
     * @return this builder instance
     */
    B dateTimeParser(DateTimeParser<T> dateTimeParser);

    /**
     * @param focusLostBehaviour the focus lost behaviour, JFormattedTextField.COMMIT by default
     * @return this builder instance
     * @see JFormattedTextField#COMMIT
     * @see JFormattedTextField#COMMIT_OR_REVERT
     * @see JFormattedTextField#REVERT
     * @see JFormattedTextField#PERSIST
     */
    B focusLostBehaviour(int focusLostBehaviour);

    /**
     * @param initialValue the initial value
     * @return this builder instance
     */
    B initialValue(T initialValue);

    /**
     * @return a new {@link TemporalField} instance
     */
    TemporalField<T> build();
  }

  private static final class DefaultBuilder<T extends Temporal, B extends Builder<T, B>> implements Builder<T, B> {

    private final Class<T> temporalClass;
    private final String dateTimePattern;

    private DateTimeFormatter dateTimeFormatter;
    private DateTimeParser<T> dateTimeParser;
    private T initialValue;
    private int focusLostBehaviour = JFormattedTextField.COMMIT;

    private DefaultBuilder(Class<T> temporalClass, String dateTimePattern) {
      this.temporalClass = requireNonNull(temporalClass);
      this.dateTimePattern = requireNonNull(dateTimePattern);
      this.dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern);
      this.dateTimeParser = initializeDateTimeParser(temporalClass);
    }

    @Override
    public B dateTimeFormatter(DateTimeFormatter dateTimeFormatter) {
      this.dateTimeFormatter = requireNonNull(dateTimeFormatter);
      return (B) this;
    }

    @Override
    public B dateTimeParser(DateTimeParser<T> dateTimeParser) {
      this.dateTimeParser = requireNonNull(dateTimeParser);
      return (B) this;
    }

    @Override
    public B focusLostBehaviour(int focusLostBehaviour) {
      this.focusLostBehaviour = focusLostBehaviour;
      return (B) this;
    }

    @Override
    public B initialValue(T initialValue) {
      this.initialValue = initialValue;
      return (B) this;
    }

    @Override
    public TemporalField<T> build() {
      if (dateTimeParser == null) {
        throw new IllegalStateException("dateTimeParser must be specified");
      }

      TemporalField<T> temporalField = new TemporalField<>(this);
      temporalField.setTemporal(initialValue);

      return temporalField;
    }

    private static <T extends Temporal> DateTimeParser<T> initializeDateTimeParser(Class<T> typeClass) {
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

      return null;
    }
  }

  private static MaskFormatter initializeFormatter(String dateTimePattern) {
    try {
      return FieldFormatter.create(LocaleDateTimePattern.getMask(dateTimePattern), true);
    }
    catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }
}
