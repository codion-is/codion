/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import is.codion.common.DateTimeParser;
import is.codion.common.event.EventDataListener;
import is.codion.common.formats.LocaleDateTimePattern;
import is.codion.common.value.Value;
import is.codion.swing.common.model.textfield.DocumentAdapter;
import is.codion.swing.common.ui.component.ComponentValue;
import is.codion.swing.common.ui.component.UpdateOn;

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
 * @see #builder(Class)
 * @param <T> the temporal type
 */
public final class TemporalField<T extends Temporal> extends JFormattedTextField {

  private final Class<T> temporalClass;
  private final String dateTimePattern;
  private final DateTimeFormatter formatter;
  private final DateTimeParser<T> dateTimeParser;
  private final Value<T> value = Value.value();

  private TemporalField(final DefaultBuilder<T> builder) {
    super(initializeFormatter(builder.dateTimePattern));
    this.temporalClass = builder.temporalClass;
    this.dateTimePattern = builder.dateTimePattern;
    this.formatter = requireNonNull(builder.dateTimeFormatter, "dateTimeFormatter");
    this.dateTimeParser = requireNonNull(builder.dateTimeParser, "dateTimeParser");
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
    catch (final DateTimeParseException e) {
      return null;
    }
  }

  /**
   * Sets the temporal value in this field, clears the field if {@code temporal} is null.
   * @param temporal the temporal value to set
   */
  public void setTemporal(final Temporal temporal) {
    setText(temporal == null ? "" : formatter.format(temporal));
  }

  /**
   * @param listener notified each time the value changes
   */
  public void addTemporalListener(final EventDataListener<T> listener) {
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
   * Instantiates a new {@link ComponentValue} for {@link Temporal} values.
   * @return a Value bound to the given component
   */
  public ComponentValue<T, TemporalField<T>> componentValue() {
    return componentValue(UpdateOn.KEYSTROKE);
  }

  /**
   * Instantiates a new {@link ComponentValue} for {@link Temporal} values.
   * @param updateOn specifies when the underlying value should be updated
   * @return a Value bound to the given component
   */
  public ComponentValue<T, TemporalField<T>> componentValue(final UpdateOn updateOn) {
    return new TemporalFieldValue<>(this, updateOn);
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
     * Note that setting the date/time pattern replaces any {@link #dateTimeFormatter(DateTimeFormatter)} that has been previously set.
     * @param dateTimePattern the date/time pattern
     * @return this builder instance
     */
    Builder<T> dateTimePattern(String dateTimePattern);

    /**
     * Use this to set the actual {@link DateTimeFormatter} to use, by default an instance based on the dateTimePattern is created.
     * Note that you must also set the dateTimePatter and the dateTimeFormatter is assumed to be able to parse that pattern.
     * @param dateTimeFormatter the date/time formatter
     * @return this builder instance
     */
    Builder<T> dateTimeFormatter(DateTimeFormatter dateTimeFormatter);

    /**
     * @param dateTimeParser the date/time parser
     * @return this builder instance
     */
    Builder<T> dateTimeParser(DateTimeParser<T> dateTimeParser);

    /**
     * @param focusLostBehaviour the focus lost behaviour, JFormattedTextField.COMMIT by default
     * @return this builder instance
     * @see JFormattedTextField#COMMIT
     * @see JFormattedTextField#COMMIT_OR_REVERT
     * @see JFormattedTextField#REVERT
     * @see JFormattedTextField#PERSIST
     */
    Builder<T> focusLostBehaviour(int focusLostBehaviour);

    /**
     * @param initialValue the initial value
     * @return this builder instance
     */
    Builder<T> initialValue(T initialValue);

    /**
     * @return a new {@link TemporalField} instance
     */
    TemporalField<T> build();
  }

  private static final class DefaultBuilder<T extends Temporal> implements Builder<T> {

    private final Class<T> temporalClass;

    private String dateTimePattern;
    private DateTimeFormatter dateTimeFormatter;
    private DateTimeParser<T> dateTimeParser;
    private T initialValue;
    private int focusLostBehaviour = JFormattedTextField.COMMIT;

    private DefaultBuilder(final Class<T> temporalClass) {
      this.temporalClass = temporalClass;
      this.dateTimeParser = initializeDateTimeParser(temporalClass);
    }

    @Override
    public Builder<T> dateTimePattern(final String dateTimePattern) {
      this.dateTimePattern = requireNonNull(dateTimePattern);
      this.dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern);
      return this;
    }

    @Override
    public Builder<T> dateTimeFormatter(final DateTimeFormatter dateTimeFormatter) {
      this.dateTimeFormatter = requireNonNull(dateTimeFormatter);
      return this;
    }

    @Override
    public Builder<T> dateTimeParser(final DateTimeParser<T> dateTimeParser) {
      this.dateTimeParser = requireNonNull(dateTimeParser);
      return this;
    }

    @Override
    public Builder<T> focusLostBehaviour(final int focusLostBehaviour) {
      this.focusLostBehaviour = focusLostBehaviour;
      return this;
    }

    @Override
    public Builder<T> initialValue(final T initialValue) {
      this.initialValue = initialValue;
      return this;
    }

    @Override
    public TemporalField<T> build() {
      if (dateTimePattern == null) {
        throw new IllegalStateException("dateTimePattern must be specified");
      }
      if (dateTimeParser == null) {
        throw new IllegalStateException("dateTimeParser must be specified");
      }

      final TemporalField<T> temporalField = new TemporalField<>(this);
      temporalField.setTemporal(initialValue);

      return temporalField;
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

      return null;
    }
  }

  private static MaskFormatter initializeFormatter(final String dateTimePattern) {
    try {
      return FieldFormatter.create(LocaleDateTimePattern.getMask(dateTimePattern), true);
    }
    catch (final ParseException e) {
      throw new RuntimeException(e);
    }
  }

  private static final class TemporalFieldValue<T extends Temporal> extends FormattedTextComponentValue<T, TemporalField<T>> {

    private TemporalFieldValue(final TemporalField<T> textComponent, final UpdateOn updateOn) {
      super(textComponent, null, updateOn);
    }

    @Override
    protected String formatTextFromValue(final T value) {
      return getComponent().getDateTimeFormatter().format(value);
    }

    @Override
    protected T parseValueFromText(final String text) {
      try {
        return getComponent().getDateTimeParser().parse(text, getComponent().getDateTimeFormatter());
      }
      catch (final DateTimeParseException e) {
        return null;
      }
    }
  }
}
