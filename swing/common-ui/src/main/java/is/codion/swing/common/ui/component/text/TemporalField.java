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
 * Copyright (c) 2021 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.format.LocaleDateTimePattern;
import is.codion.common.observer.Observable;
import is.codion.common.resource.MessageBundle;
import is.codion.common.state.ObservableState;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.text.DocumentAdapter;
import is.codion.swing.common.ui.component.calendar.CalendarPanel;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ControlKey;
import is.codion.swing.common.ui.control.ControlMap;
import is.codion.swing.common.ui.dialog.Dialogs;

import org.jspecify.annotations.Nullable;

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

import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.component.text.TemporalField.ControlKeys.*;
import static is.codion.swing.common.ui.control.ControlMap.controlMap;
import static is.codion.swing.common.ui.key.KeyEvents.keyStroke;
import static java.awt.event.KeyEvent.*;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;

/**
 * A JFormattedTextField for Temporal types.
 * <p>
 * Use {@link #get()} and {@link #set(Temporal)} for accessing and setting the value.
 * @param <T> the temporal type
 * @see #builder()
 */
public final class TemporalField<T extends Temporal> extends JFormattedTextField {

	private static final MessageBundle MESSAGES =
					messageBundle(TemporalField.class, getBundle(TemporalField.class.getName()));

	/**
	 * The controls.
	 */
	public static final class ControlKeys {

		/**
		 * Display a calendar for date/time input.<br>
		 * Default key stroke: INSERT
		 */
		public static final ControlKey<CommandControl> DISPLAY_CALENDAR = CommandControl.key("displayCalendar", keyStroke(VK_INSERT));
		/**
		 * Increments the date component under the cursor.<br>
		 * Default key stroke: UP ARROW
		 */
		public static final ControlKey<CommandControl> INCREMENT = CommandControl.key("increment", keyStroke(VK_UP));
		/**
		 * Decrements the date component under the cursor.<br>
		 * Default key stroke: DOWN ARROW
		 */
		public static final ControlKey<CommandControl> DECREMENT = CommandControl.key("decrement", keyStroke(VK_DOWN));

		private ControlKeys() {}
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
	private final Value<T> value = Value.nullable();
	private final State valueNull = State.state();
	private final String dateTimePattern;
	private final @Nullable ImageIcon calendarIcon;
	private final ControlMap controlMap;

	private TemporalField(DefaultBuilder<T> builder) {
		super(createFormatter(builder.mask));
		setToolTipText(builder.dateTimePattern);
		this.temporalClass = builder.temporalClass;
		this.formatter = builder.dateTimeFormatter;
		this.dateTimeParser = builder.dateTimeParser;
		this.dateTimePattern = builder.dateTimePattern;
		this.calendarIcon = builder.calendarIcon;
		this.controlMap = builder.controlMap;
		this.controlMap.control(INCREMENT).set(Control.builder()
						.command(this::increment)
						.enabled(valueNull.not())
						.build());
		this.controlMap.control(DECREMENT).set(Control.builder()
						.command(this::decrement)
						.enabled(valueNull.not())
						.build());
		this.controlMap.control(DISPLAY_CALENDAR).set(createCalendarControl());
		if (builder.incrementDecrementEnabled) {
			this.controlMap.keyEvent(INCREMENT).ifPresent(keyEvent -> keyEvent.enable(this));
			this.controlMap.keyEvent(DECREMENT).ifPresent(keyEvent -> keyEvent.enable(this));
		}
		this.controlMap.keyEvent(DISPLAY_CALENDAR).ifPresent(keyEvent -> keyEvent.enable(this));
		setFocusLostBehavior(builder.focusLostBehaviour);
		getDocument().addDocumentListener(new TemporalDocumentListener());
	}

	/**
	 * Returns a Control for displaying a calendar, an empty Optional
	 * in case a Calendar is not supported for the given temporal type
	 * @return a Control for displaying a calendar
	 */
	public Optional<CommandControl> calendarControl() {
		return controlMap.control(DISPLAY_CALENDAR).optional();
	}

	/**
	 * @return the Temporal class this field is based on
	 */
	public Class<T> temporalClass() {
		return temporalClass;
	}

	/**
	 * @return the temporal value currently being displayed, an empty Optional in case of an incomplete/unparseable date
	 */
	public Optional<T> optional() {
		return value.optional();
	}

	/**
	 * @return the Temporal value currently being displayed, null in case of an incomplete/unparseable date
	 */
	public @Nullable T get() {
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
	public void set(@Nullable Temporal temporal) {
		setText(temporal == null ? "" : formatter.format(temporal));
	}

	/**
	 * @return an {@link Observable} notified each time the value changes
	 */
	public Observable<T> observable() {
		return value.observable();
	}

	/**
	 * @return a {@link Builder.TemporalClassStep}
	 */
	public static Builder.TemporalClassStep builder() {
		return DefaultBuilder.TEMPORAL_CLASS;
	}

	private void increment() {
		increment(1);
	}

	private void decrement() {
		increment(-1);
	}

	private void increment(int amount) {
		int caretPosition = getCaretPosition();
		T temporal = get();
		if (temporal != null && caretPosition <= dateTimePattern.length()) {
			char patternCharacter = caretPosition == dateTimePattern.length() ?
							dateTimePattern.charAt(dateTimePattern.length() - 1) :
							dateTimePattern.charAt(caretPosition);
			ChronoUnit chronoUnit = chronoUnit(patternCharacter);
			if (chronoUnit != null) {
				set(temporal.plus(amount, chronoUnit));
				setCaretPosition(caretPosition);
			}
		}
	}

	private static @Nullable ChronoUnit chronoUnit(char patternCharacter) {
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

	private @Nullable CommandControl createCalendarControl() {
		if (CalendarPanel.supportedTypes().contains(temporalClass)) {
			return Control.builder()
							.command(this::displayCalendar)
							.caption(calendarIcon == null ? "..." : null)
							.smallIcon(calendarIcon)
							.description(MESSAGES.getString("display_calendar"))
							.enabled(calendarEnabledState())
							.build();
		}

		return null;
	}

	private ObservableState calendarEnabledState() {
		State enabledState = State.state(isEnabled());
		addPropertyChangeListener("enabled", event -> enabledState.set((Boolean) event.getNewValue()));

		return enabledState.observable();
	}

	private void displayCalendar() {
		if (LocalDate.class.equals(temporalClass())) {
			Dialogs.calendar()
							.owner(this)
							.icon(calendarIcon)
							.value((LocalDate) get())
							.selectLocalDate()
							.ifPresent(this::set);
		}
		else if (LocalDateTime.class.equals(temporalClass())) {
			Dialogs.calendar()
							.owner(this)
							.icon(calendarIcon)
							.value((LocalDateTime) get())
							.selectLocalDateTime()
							.ifPresent(this::set);
		}
		else {
			throw new IllegalArgumentException("Unsupported temporal type: " + temporalClass());
		}
	}

	private final class TemporalDocumentListener implements DocumentAdapter {

		@Override
		public void contentsChanged(DocumentEvent e) {
			T temporal = get();
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
		 * Provides a {@link Builder}
		 */
		interface TemporalClassStep {

			/**
			 * A builder for {@link TemporalField}.
			 * This builder supports: {@link LocalTime}, {@link LocalDate}, {@link LocalDateTime}, {@link OffsetDateTime},<br>
			 * for other {@link Temporal} types use {@link Builder#dateTimeParser} to supply a {@link DateTimeParser} instance.
			 * @param temporalClass the temporal class
			 * @param <T> the temporal type
			 * @return a new builder
			 */
			<T extends Temporal> Builder<T> temporalClass(Class<T> temporalClass);
		}

		/**
		 * @param dateTimePattern the date time pattern
		 * @return this builder instance
		 */
		Builder<T> dateTimePattern(String dateTimePattern);

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
		 * @param controlKey the control key
		 * @param keyStroke the keyStroke to assign to the given control
		 * @return this builder instance
		 */
		Builder<T> keyStroke(ControlKey<?> controlKey, KeyStroke keyStroke);
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

	private static final class DefaultTemporalClassStep implements Builder.TemporalClassStep {

		@Override
		public <T extends Temporal> Builder<T> temporalClass(Class<T> temporalClass) {
			return new DefaultBuilder<>(temporalClass);
		}
	}

	private static final class DefaultBuilder<T extends Temporal>
					extends DefaultTextFieldBuilder<T, TemporalField<T>, Builder<T>> implements Builder<T> {

		private static final Builder.TemporalClassStep TEMPORAL_CLASS = new DefaultTemporalClassStep();

		private final Class<T> temporalClass;
		private final ControlMap controlMap = controlMap(ControlKeys.class);

		private String dateTimePattern;
		private DateTimeFormatter dateTimeFormatter;
		private String mask;
		private DateTimeParser<T> dateTimeParser;
		private int focusLostBehaviour = JFormattedTextField.COMMIT;
		private @Nullable ImageIcon calendarIcon;
		private boolean incrementDecrementEnabled = true;

		private DefaultBuilder(Class<T> temporalClass) {
			super(temporalClass);
			this.temporalClass = requireNonNull(temporalClass);
			this.dateTimeParser = createDateTimeParser(temporalClass);
			dateTimePattern(defaultDateTimePattern());
		}

		@Override
		public Builder<T> dateTimePattern(String dateTimePattern) {
			this.dateTimePattern = requireNonNull(dateTimePattern);
			this.dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern)
							.withZone(ZoneId.systemDefault());
			this.mask = createMask(dateTimePattern);
			return this;
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
		public Builder<T> keyStroke(ControlKey<?> controlKey, KeyStroke keyStroke) {
			controlMap.keyStroke(controlKey).set(keyStroke);
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
			return new TemporalFieldValue<>(component, updateOn());
		}

		private String defaultDateTimePattern() {
			if (temporalClass.equals(LocalTime.class)) {
				return "HH:mm";
			}
			else if (temporalClass.equals(LocalDate.class)) {
				return LocaleDateTimePattern.builder()
								.build()
								.dateTimePattern();
			}

			return LocaleDateTimePattern.builder()
							.hoursMinutes()
							.build()
							.dateTimePattern();
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

			throw new IllegalArgumentException("Unsupported temporal class: " + valueClass);
		}

		/**
		 * Parses the given date/time pattern and returns a mask string that can be used in JFormattedFields.
		 * This only works with plain numerical date formats.
		 * @param dateTimePattern the format pattern for which to create the mask
		 * @return a String representing the mask to use in JFormattedTextFields, i.e. "##-##-####"
		 */
		private static String createMask(String dateTimePattern) {
			StringBuilder stringBuilder = new StringBuilder(requireNonNull(dateTimePattern).length());
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
