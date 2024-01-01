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
package is.codion.swing.common.ui.component.text;

import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.text.DocumentAdapter;
import is.codion.swing.common.ui.component.calendar.CalendarPanel;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.common.ui.key.KeyboardShortcuts;

import javax.swing.ImageIcon;
import javax.swing.JFormattedTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentEvent.EventType;
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
import java.util.function.Function;

import static is.codion.swing.common.ui.component.text.TemporalField.KeyboardShortcut.*;
import static is.codion.swing.common.ui.key.KeyboardShortcuts.keyStroke;
import static is.codion.swing.common.ui.key.KeyboardShortcuts.keyboardShortcuts;
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

  public static final KeyboardShortcuts<KeyboardShortcut> KEYBOARD_SHORTCUTS = keyboardShortcuts(KeyboardShortcut.class, new DefaultKeyboardShortcuts());

  /**
   * The available keyboard shortcuts.
   */
  public enum KeyboardShortcut {
    DISPLAY_CALENDAR,
    INCREMENT,
    DECREMENT
  }

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
  private final State valueNull = State.state();
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
    setFocusLostBehavior(builder.focusLostBehaviour);
    getDocument().addDocumentListener(new TemporalDocumentListener());
    if (builder.incrementDecrementEnabled) {
      KeyEvents.builder(builder.keyboardShortcuts.keyStroke(INCREMENT).get())
              .action(Control.builder(this::increment)
                      .enabled(valueNull.not())
                      .build())
              .enable(this);
      KeyEvents.builder(builder.keyboardShortcuts.keyStroke(DECREMENT).get())
              .action(Control.builder(this::decrement)
                      .enabled(valueNull.not())
                      .build())
              .enable(this);
    }
    calendarControl = createCalendarControl();
    if (calendarControl != null) {
      KeyEvents.builder(builder.keyboardShortcuts.keyStroke(DISPLAY_CALENDAR).get())
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

  private final class TemporalDocumentListener implements DocumentAdapter {

    @Override
    public void contentsChanged(DocumentEvent e) {
      T temporal = getTemporal();
      // preventing the value from becoming null on every single edit, since replace
      // is implemented as a remove and insert, the remove resulting in a null value,
      // so we only set the value to null on insert
      if (temporal != null || e.getType() == EventType.INSERT) {
        value.set(temporal);
        valueNull.set(temporal == null);
      }
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

    /**
     * @param incrementDecrementEnabled enable increment/decrement of date component under cursor
     * @return this builder instance
     */
    Builder<T> incrementDecrementEnabled(boolean incrementDecrementEnabled);

    /**
     * @param keyboardShortcut the keyboard shortcut key
     * @param keyStroke the keyStroke to assign to the given shortcut key, null resets to the default one
     * @return this builder instance
     */
    Builder<T> keyStroke(KeyboardShortcut keyboardShortcut, KeyStroke keyStroke);
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
    private final KeyboardShortcuts<KeyboardShortcut> keyboardShortcuts = KEYBOARD_SHORTCUTS.copy();

    private DateTimeFormatter dateTimeFormatter;
    private DateTimeParser<T> dateTimeParser;
    private int focusLostBehaviour = JFormattedTextField.COMMIT;
    private ImageIcon calendarIcon;
    private boolean incrementDecrementEnabled = true;

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
    public Builder<T> incrementDecrementEnabled(boolean incrementDecrementEnabled) {
      this.incrementDecrementEnabled = incrementDecrementEnabled;
      return this;
    }

    @Override
    public Builder<T> keyStroke(KeyboardShortcut keyboardShortcut, KeyStroke keyStroke) {
      keyboardShortcuts.keyStroke(keyboardShortcut).set(keyStroke);
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

  private static final class DefaultKeyboardShortcuts implements Function<KeyboardShortcut, KeyStroke> {

    @Override
    public KeyStroke apply(KeyboardShortcut shortcut) {
      switch (shortcut) {
        case DISPLAY_CALENDAR: return keyStroke(VK_INSERT);
        case INCREMENT: return keyStroke(VK_UP);
        case DECREMENT: return keyStroke(VK_DOWN);
        default: throw new IllegalArgumentException();
      }
    }
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
}
