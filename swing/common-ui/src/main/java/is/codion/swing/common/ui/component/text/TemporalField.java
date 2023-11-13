/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.text.DocumentAdapter;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.component.calendar.CalendarPanel;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;

import javax.swing.ImageIcon;
import javax.swing.JFormattedTextField;
import javax.swing.text.MaskFormatter;
import java.text.ParseException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static java.awt.event.KeyEvent.*;
import static java.util.Objects.requireNonNull;

/**
 * A JFormattedTextField for Temporal types.<br>
 * Use {@link #getTemporal()} and {@link #setTemporal(Temporal)} for accessing and setting the value.
 * @param <T> the temporal type
 * @see #builder(Class, String, Value)
 */
public final class TemporalField<T extends Temporal> extends JFormattedTextField {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(TemporalField.class.getName());

  private static final char DAY = 'd';
  private static final char MONTH = 'M';
  private static final char YEAR = 'y';
  private static final char HOUR = 'H';
  private static final char MINUTE = 'm';
  private static final char SECOND = 's';

  private final Class<T> temporalClass;
  private final DateTimeFormatter formatter;
  private final DateTimeParser<T> dateTimeParser;
  private final Value<T> value = Value.value();
  private final State valueNotNullState = State.state(false);
  private final String dateTimePattern;
  private final ImageIcon calendarIcon;
  private final Control calendarControl;

  private TemporalField(DefaultBuilder<T> builder) {
    super(createFormatter(builder.mask));
    setToolTipText(builder.dateTimePattern);
    this.temporalClass = builder.temporalClass;
    this.formatter = builder.dateTimeFormatter;
    this.dateTimeParser = builder.dateTimeParser;
    this.dateTimePattern = builder.dateTimePattern;
    this.calendarIcon = builder.calendarIcon;
    this.value.addDataListener(temporal -> valueNotNullState.set(temporal != null));
    setFocusLostBehavior(builder.focusLostBehaviour);
    getDocument().addDocumentListener((DocumentAdapter) e -> value.set(getTemporal()));
    KeyEvents.builder(VK_UP)
            .action(Control.builder(this::increment)
                    .enabled(valueNotNullState)
                    .build())
            .enable(this);
    KeyEvents.builder(VK_DOWN)
            .action(Control.builder(this::decrement)
                    .enabled(valueNotNullState)
                    .build())
            .enable(this);
    calendarControl = createCalendarControl();
    if (calendarControl != null) {
      KeyEvents.builder(VK_INSERT)
              .action(calendarControl)
              .enable(this);
    }
  }

  /**
   * Returns a Control for displaying a calendar, an empty Optional
   * in case a Calendar is not supported for the given temporal type
   * @return a Control for displaying a calendar
   */
  public Optional<Control> calendarControl() {
    return Optional.ofNullable(calendarControl);
  }

  /**
   * @return the Temporal class this field is based on
   */
  public Class<T> temporalClass() {
    return temporalClass;
  }

  /**
   * @return the Temporal value currently being displayed, an empty Optional in case of an incomplete/unparseable date
   */
  public Optional<T> optional() {
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
  public void addListener(Consumer<T> listener) {
    value.addDataListener(listener);
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
  public static <T extends Temporal> Builder<T> builder(Class<T> temporalClass, String dateTimePattern) {
    return new DefaultBuilder<>(temporalClass, dateTimePattern, null);
  }

  /**
   * A builder for {@link TemporalField}.
   * This builder supports: {@link LocalTime}, {@link LocalDate}, {@link LocalDateTime}, {@link OffsetDateTime},<br>
   * for other {@link Temporal} types use {@link Builder#dateTimeParser} to supply a {@link DateTimeParser} instance.
   * @param temporalClass the temporal class
   * @param dateTimePattern the date time pattern
   * @param linkedValue the value to link to the component
   * @param <T> the temporal type
   * @return a new builder
   */
  public static <T extends Temporal> Builder<T> builder(Class<T> temporalClass, String dateTimePattern,
                                                        Value<T> linkedValue) {
    return new DefaultBuilder<>(temporalClass, dateTimePattern, requireNonNull(linkedValue));
  }

  private void increment() {
    increment(1);
  }

  private void decrement() {
    increment(-1);
  }

  private void increment(int amount) {
    int caretPosition = getCaretPosition();
    T temporal = getTemporal();
    if (temporal != null && caretPosition <= dateTimePattern.length()) {
      char patternCharacter = caretPosition == dateTimePattern.length() ?
              dateTimePattern.charAt(dateTimePattern.length() - 1) :
              dateTimePattern.charAt(caretPosition);
      ChronoUnit chronoUnit = chronoUnit(patternCharacter);
      if (chronoUnit != null) {
        setTemporal(temporal.plus(amount, chronoUnit));
        setCaretPosition(caretPosition);
      }
    }
  }

  private static ChronoUnit chronoUnit(char patternCharacter) {
    switch (patternCharacter) {
      case DAY:
        return ChronoUnit.DAYS;
      case MONTH:
        return ChronoUnit.MONTHS;
      case YEAR:
        return ChronoUnit.YEARS;
      case HOUR:
        return ChronoUnit.HOURS;
      case MINUTE:
        return ChronoUnit.MINUTES;
      case SECOND:
        return ChronoUnit.SECONDS;
      default:
        return null;
    }
  }

  private Control createCalendarControl() {
    if (CalendarPanel.supportedTypes().contains(temporalClass)) {
      return Control.builder(this::displayCalendar)
              .name(calendarIcon == null ? "..." : null)
              .smallIcon(calendarIcon)
              .description(MESSAGES.getString("display_calendar"))
              .enabled(calendarEnabledState())
              .build();
    }

    return null;
  }

  private StateObserver calendarEnabledState() {
    State enabledState = State.state(isEnabled());
    addPropertyChangeListener("enabled", event -> enabledState.set((Boolean) event.getNewValue()));

    return enabledState.observer();
  }

  private void displayCalendar() {
    if (LocalDate.class.equals(temporalClass())) {
      Dialogs.calendarDialog()
              .owner(this)
              .icon(calendarIcon)
              .initialValue((LocalDate) getTemporal())
              .selectLocalDate()
              .ifPresent(this::setTemporal);
    }
    else if (LocalDateTime.class.equals(temporalClass())) {
      Dialogs.calendarDialog()
              .owner(this)
              .icon(calendarIcon)
              .initialValue((LocalDateTime) getTemporal())
              .selectLocalDateTime()
              .ifPresent(this::setTemporal);
    }
    else {
      throw new IllegalArgumentException("Unsupported temporal type: " + temporalClass());
    }
  }

  /**
   * A builder for {@link TemporalField}.
   * @param <T> the temporal type
   */
  public interface Builder<T extends Temporal> extends TextFieldBuilder<T, TemporalField<T>, Builder<T>> {

    /**
     * Sets the {@link DateTimeFormatter} for this field, this formatter must
     * be able to parse the date time pattern this field is based on.
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
     * @param calendarIcon the calendar icon
     * @return this builder instance
     */
    Builder<T> calendarIcon(ImageIcon calendarIcon);
  }

  /**
   * Parses a Temporal value from text with a provided formatter
   * @param <T> the Temporal type
   */
  public interface DateTimeParser<T extends Temporal> {

    /**
     * Parses the given text with the given formatter
     * @param text the text to parse
     * @param formatter the formatter to use
     * @return the Temporal value
     * @throws DateTimeParseException if unable to parse the text
     */
    T parse(CharSequence text, DateTimeFormatter formatter);
  }

  private static final class DefaultBuilder<T extends Temporal>
          extends DefaultTextFieldBuilder<T, TemporalField<T>, Builder<T>> implements Builder<T> {

    private final Class<T> temporalClass;
    private final String dateTimePattern;
    private final String mask;

    private DateTimeFormatter dateTimeFormatter;
    private DateTimeParser<T> dateTimeParser;
    private int focusLostBehaviour = JFormattedTextField.COMMIT;
    private ImageIcon calendarIcon;

    private DefaultBuilder(Class<T> temporalClass, String dateTimePattern, Value<T> linkedValue) {
      super(temporalClass, linkedValue);
      this.temporalClass = temporalClass;
      this.dateTimePattern = requireNonNull(dateTimePattern);
      this.dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern).withZone(ZoneId.systemDefault());
      this.mask = createMask(dateTimePattern);
      this.dateTimeParser = createDateTimeParser(temporalClass);
    }

    @Override
    public Builder<T> dateTimeFormatter(DateTimeFormatter dateTimeFormatter) {
      this.dateTimeFormatter = requireNonNull(dateTimeFormatter);
      return this;
    }

    @Override
    public Builder<T> dateTimeParser(DateTimeParser<T> dateTimeParser) {
      this.dateTimeParser = requireNonNull(dateTimeParser);
      return this;
    }

    @Override
    public Builder<T> focusLostBehaviour(int focusLostBehaviour) {
      this.focusLostBehaviour = focusLostBehaviour;
      return this;
    }

    @Override
    public Builder<T> calendarIcon(ImageIcon calendarIcon) {
      this.calendarIcon = calendarIcon;
      return this;
    }

    @Override
    protected TemporalField<T> createTextField() {
      if (dateTimeParser == null) {
        throw new IllegalStateException("dateTimeParser must be specified");
      }

      return new TemporalField<>(this);
    }

    @Override
    protected ComponentValue<T, TemporalField<T>> createComponentValue(TemporalField<T> component) {
      return new TemporalFieldValue<>(component, updateOn);
    }

    @Override
    protected void setInitialValue(TemporalField<T> component, T initialValue) {
      component.setTemporal(initialValue);
    }

    private static <T extends Temporal> DateTimeParser<T> createDateTimeParser(Class<T> valueClass) {
      if (valueClass.equals(LocalTime.class)) {
        return (DateTimeParser<T>) new LocalTimeParser();
      }
      else if (valueClass.equals(LocalDate.class)) {
        return (DateTimeParser<T>) new LocalDateParser();
      }
      else if (valueClass.equals(LocalDateTime.class)) {
        return (DateTimeParser<T>) new LocalDateTimeParser();
      }
      else if (valueClass.equals(OffsetDateTime.class)) {
        return (DateTimeParser<T>) new OffsetDateTimeParser();
      }

      return null;
    }

    /**
     * Parses the given date/time pattern and returns a mask string that can be used in JFormattedFields.
     * This only works with plain numerical date formats.
     * @param dateTimePattern the format pattern for which to create the mask
     * @return a String representing the mask to use in JFormattedTextFields, i.e. "##-##-####"
     */
    private static String createMask(String dateTimePattern) {
      requireNonNull(dateTimePattern, "dateTimePattern");
      StringBuilder stringBuilder = new StringBuilder(dateTimePattern.length());
      for (Character character : dateTimePattern.toCharArray()) {
        stringBuilder.append(Character.isLetter(character) ? "#" : character);
      }

      return stringBuilder.toString();
    }
  }

  private static MaskFormatter createFormatter(String mask) {
    try {
      return MaskFormatterBuilder.builder()
              .mask(mask)
              .placeholderCharacter('_')
              .valueContainsLiteralCharacters(true)
              .commitsOnValidEdit(true)
              .build();
    }
    catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  private static boolean supportsCalendar(Class<?> temporalClass) {
    return LocalDate.class.equals(temporalClass) || LocalDateTime.class.equals(temporalClass);
  }

  private static final class LocalTimeParser implements DateTimeParser<LocalTime> {

    @Override
    public LocalTime parse(CharSequence text, DateTimeFormatter formatter) {
      return LocalTime.parse(text, formatter);
    }
  }

  private static final class LocalDateParser implements DateTimeParser<LocalDate> {

    @Override
    public LocalDate parse(CharSequence text, DateTimeFormatter formatter) {
      return LocalDate.parse(text, formatter);
    }
  }

  private static final class LocalDateTimeParser implements DateTimeParser<LocalDateTime> {

    @Override
    public LocalDateTime parse(CharSequence text, DateTimeFormatter formatter) {
      return LocalDateTime.parse(text, formatter);
    }
  }

  private static final class OffsetDateTimeParser implements DateTimeParser<OffsetDateTime> {

    @Override
    public OffsetDateTime parse(CharSequence text, DateTimeFormatter formatter) {
      try {
        return OffsetDateTime.parse(text, formatter);
      }
      catch (DateTimeException e) {
        //assuming 'Unable to obtain OffsetDateTime from TemporalAccessor', let's fall back
        //to LocalDateTime with the zone from the formatter or system default if none is specified
        ZoneId zone = formatter.getZone();
        if (zone == null) {
          zone = ZoneId.systemDefault();
        }

        return LocalDateTime.parse(text, formatter).atZone(zone).toOffsetDateTime();
      }
    }
  }

  public static void main(String... args) {
    Value<LocalDate> value = Value.value();
    ComponentValue<LocalDate, TemporalField<LocalDate>> builtValue = new DefaultBuilder<>(LocalDate.class, "dd.MM.yyyy", value)
            .columns(10)
            .buildValue();
    value.addDataListener(System.out::println);
    Dialogs.componentDialog(builtValue.component())
            .show();
  }
}
