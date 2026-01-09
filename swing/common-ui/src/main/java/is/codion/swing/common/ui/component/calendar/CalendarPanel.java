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
 * Copyright (c) 2021 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.calendar;

import is.codion.common.reactive.event.Event;
import is.codion.common.reactive.observer.Observable;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.common.utilities.item.Item;
import is.codion.common.utilities.property.PropertyValue;
import is.codion.common.utilities.resource.MessageBundle;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.panel.FlexibleGridLayoutPanelBuilder;
import is.codion.swing.common.ui.component.panel.GridLayoutPanelBuilder;
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ControlKey;
import is.codion.swing.common.ui.control.ControlMap;
import is.codion.swing.common.ui.dialog.Dialogs;

import org.jspecify.annotations.Nullable;

import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.WeekFields;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static is.codion.common.utilities.Configuration.booleanValue;
import static is.codion.common.utilities.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.border.Borders.emptyBorder;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.component.calendar.CalendarPanel.ControlKeys.*;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.control.ControlMap.controlMap;
import static is.codion.swing.common.ui.key.KeyEvents.MENU_SHORTCUT_MASK;
import static is.codion.swing.common.ui.key.KeyEvents.keyStroke;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager;
import static java.awt.event.InputEvent.SHIFT_DOWN_MASK;
import static java.awt.event.KeyEvent.*;
import static java.time.ZoneId.getAvailableZoneIds;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.swing.BorderFactory.createEtchedBorder;
import static javax.swing.BorderFactory.createTitledBorder;
import static javax.swing.SwingUtilities.invokeLater;
import static javax.swing.SwingUtilities.isEventDispatchThread;

/**
 * A panel presenting a calendar for date/time selection.
 * <p>
 * Keyboard navigation:
 * <ul>
 * <li>Previous/next year: CTRL + left/right arrow or down/up arrow.
 * <li>Previous/next month: SHIFT + left/right arrow or down/up arrow.
 * <li>Previous/next week: up/down arrow.
 * <li>Previous/next day: left/right arrow.
 * <li>Previous/next hour: SHIFT-ALT + left/right arrow or down/up arrow.
 * <li>Previous/next minute: CTRL-ALT + left/right arrow or down/up arrow.
 * </ul>
 * @see #builder()
 */
public final class CalendarPanel extends JPanel {

	private static final MessageBundle MESSAGES =
					messageBundle(CalendarPanel.class, getBundle(CalendarPanel.class.getName()));

	/**
	 * Specifies whether CalendarPanel displays week numbers by default.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: false
	 * </ul>
	 */
	public static final PropertyValue<Boolean> WEEK_NUMBERS = booleanValue(CalendarPanel.class.getName() + ".weekNumbers", false);

	/**
	 * The available controls.
	 * <p>Note: CTRL in key stroke descriptions represents the platform menu shortcut key (CTRL on Windows/Linux, ⌘ on macOS).
	 */
	public static final class ControlKeys {

		/**
		 * Select the previous year.<br>
		 * Default key stroke: CTRL-DOWN ARROW
		 */
		public static final ControlKey<CommandControl> PREVIOUS_YEAR = CommandControl.key("previousYear", keyStroke(VK_DOWN, MENU_SHORTCUT_MASK));
		/**
		 * Select the next year.<br>
		 * Default key stroke: CTRL-UP ARROW
		 */
		public static final ControlKey<CommandControl> NEXT_YEAR = CommandControl.key("nextYear", keyStroke(VK_UP, MENU_SHORTCUT_MASK));
		/**
		 * Select the previous month.<br>
		 * Default key stroke: SHIFT-DOWN ARROW
		 */
		public static final ControlKey<CommandControl> PREVIOUS_MONTH = CommandControl.key("previousMonth", keyStroke(VK_DOWN, SHIFT_DOWN_MASK));
		/**
		 * Select the next month.<br>
		 * Default key stroke: SHIFT-UP ARROW
		 */
		public static final ControlKey<CommandControl> NEXT_MONTH = CommandControl.key("nextMonth", keyStroke(VK_UP, SHIFT_DOWN_MASK));
		/**
		 * Select the previous week.<br>
		 * Default key stroke: UP ARROW
		 */
		public static final ControlKey<CommandControl> PREVIOUS_WEEK = CommandControl.key("previousWeek", keyStroke(VK_UP));
		/**
		 * Select the next week.<br>
		 * Default key stroke: DOWN ARROW
		 */
		public static final ControlKey<CommandControl> NEXT_WEEK = CommandControl.key("nextWeek", keyStroke(VK_DOWN));
		/**
		 * Select the previous day.<br>
		 * Default key stroke: LEFT ARROW
		 */
		public static final ControlKey<CommandControl> PREVIOUS_DAY = CommandControl.key("previousDay", keyStroke(VK_LEFT));
		/**
		 * Select the next day.<br>
		 * Default key stroke: RIGHT ARROW
		 */
		public static final ControlKey<CommandControl> NEXT_DAY = CommandControl.key("nextDay", keyStroke(VK_RIGHT));
		/**
		 * Select the previous hour.<br>
		 * Default key stroke: SHIFT-ALT-DOWN ARROW
		 */
		public static final ControlKey<CommandControl> PREVIOUS_HOUR = CommandControl.key("previousHour", keyStroke(VK_DOWN, SHIFT_DOWN_MASK | ALT_DOWN_MASK));
		/**
		 * Select the next hour.<br>
		 * Default key stroke: SHIFT-ALT-UP ARROW
		 */
		public static final ControlKey<CommandControl> NEXT_HOUR = CommandControl.key("nextHour", keyStroke(VK_UP, SHIFT_DOWN_MASK | ALT_DOWN_MASK));
		/**
		 * Select the previous minute.<br>
		 * Default key stroke: CTRL-ALT-DOWN ARROW
		 */
		public static final ControlKey<CommandControl> PREVIOUS_MINUTE = CommandControl.key("previousMinute", keyStroke(VK_DOWN, MENU_SHORTCUT_MASK | ALT_DOWN_MASK));
		/**
		 * Select the next minute.<br>
		 * Default key stroke: CTRL-ALT-UP ARROW
		 */
		public static final ControlKey<CommandControl> NEXT_MINUTE = CommandControl.key("nextMinute", keyStroke(VK_UP, MENU_SHORTCUT_MASK | ALT_DOWN_MASK));

		private ControlKeys() {}
	}

	private static final Set<Class<? extends Temporal>> SUPPORTED_TYPES =
					unmodifiableSet(new HashSet<>(asList(LocalDate.class, LocalDateTime.class)));

	private static final int YEAR_COLUMNS = 4;
	private static final int TIME_COLUMNS = 2;
	private static final int DAYS_IN_WEEK = 7;
	private static final int MAX_DAYS_IN_MONTH = 31;
	private static final int DAY_GRID_ROWS = 6;
	private static final int DAY_GRID_CELLS = 42;

	private final Locale selectedLocale;
	private final DayOfWeek firstDayOfWeek;
	private final int minimalDaysInFirstWeek;
	private final DateTimeFormatter dateFormatter;
	private final DateTimeFormatter timeFormatter;

	private final DefaultCalendarDate date;
	private final DefaultCalendarDateTime dateTime;
	private final DefaultCalendarOffsetDateTime offsetDateTime;
	private final DefaultCalendarZonedDateTime zonedDateTime;

	private final Value<Integer> yearValue;
	private final Value<Month> monthValue;
	private final Value<Integer> dayValue;
	private final Value<Integer> hourValue;
	private final Value<Integer> minuteValue;
	private final Value<ZoneId> zoneIdValue;
	private final State todaySelected;

	private final UpdateDateTime updateDateTime = new UpdateDateTime();
	private final LayoutDayPanel layoutDayPanel = new LayoutDayPanel();
	private final DayMouseListener dayMouseListener = new DayMouseListener();
	private final ControlMap controlMap;
	private final List<DayOfWeek> dayColumns;
	private final Map<Integer, DayLabel> dayLabels;
	private final JPanel dayGridPanel;
	private final JLabel formattedDateLabel;
	private final boolean includeTime;
	private final boolean includeTimeZone;
	private final boolean includeTodayButton;
	private final boolean includeWeekNumbers;
	private final ObservableState enabledState;
	private final Event<Integer> doubleClicked = Event.event();
	private @Nullable JPanel weekNumberPanel;

	CalendarPanel(DefaultBuilder builder) {
		this.includeTime = builder.includeTime;
		this.includeTimeZone = builder.includeTimeZone;
		this.includeTodayButton = builder.includeTodayButton;
		this.controlMap = builder.controlMap;
		this.selectedLocale = builder.locale;
		this.firstDayOfWeek = builder.firstDayOfWeek;
		this.minimalDaysInFirstWeek = builder.minimalDaysInFirstWeek;
		this.includeWeekNumbers = builder.includeWeekNumbers;
		this.dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(selectedLocale);
		this.timeFormatter = includeTimeZone ? new DateTimeFormatterBuilder()
						.append(DateTimeFormatter.ofPattern("HH:mm, "))
						.appendZoneId()
						.toFormatter() : DateTimeFormatter.ofPattern("HH:mm");
		this.enabledState = builder.enabled;
		ZonedDateTime initialValue = builder.value == null ? ZonedDateTime.now() : builder.value;
		yearValue = Value.builder()
						.nonNull(initialValue.getYear())
						.listener(updateDateTime)
						.listener(layoutDayPanel)
						.build();
		monthValue = Value.builder()
						.nonNull(initialValue.getMonth())
						.listener(updateDateTime)
						.listener(layoutDayPanel)
						.build();
		dayValue = Value.builder()
						.nonNull(initialValue.getDayOfMonth())
						.listener(updateDateTime)
						.build();
		hourValue = Value.builder()
						.nonNull(initialValue.getHour())
						.listener(updateDateTime)
						.build();
		minuteValue = Value.builder()
						.nonNull(initialValue.getMinute())
						.listener(updateDateTime)
						.build();
		zoneIdValue = Value.builder()
						.nonNull(initialValue.getZone())
						.listener(this::updateDateTime)
						.build();
		date = new DefaultCalendarDate();
		dateTime = new DefaultCalendarDateTime();
		offsetDateTime = new DefaultCalendarOffsetDateTime();
		zonedDateTime = new DefaultCalendarZonedDateTime();
		todaySelected = State.state(todaySelected());
		dayColumns = IntStream.range(0, DAYS_IN_WEEK)
						.mapToObj(firstDayOfWeek::plus)
						.collect(toList());
		dayLabels = createDayLabels();
		dayGridPanel = new JPanel(new GridLayout(DAY_GRID_ROWS, DAYS_IN_WEEK));
		formattedDateLabel = label()
						.horizontalAlignment(SwingConstants.CENTER)
						.border(emptyBorder())
						.enabled(enabledState)
						.build();
		initializeUI();
		createControls();
		addKeyEvents();
		updateFormattedDate();
		updateDayLabelBorders();
		enabledState.addConsumer(this::enabledChanged);
	}

	/**
	 * @return the calendar date
	 */
	public CalendarDate date() {
		return date;
	}

	/**
	 * @return the calendar date and time
	 */
	public CalendarDateTime dateTime() {
		return dateTime;
	}

	/**
	 * @return the calendar zoned date time
	 */
	public CalendarZonedDateTime zonedDateTime() {
		return zonedDateTime;
	}

	/**
	 * @return the calendar offset date time
	 */
	public Observable<OffsetDateTime> offsetDateTime() {
		return offsetDateTime;
	}

	/**
	 * @return an {@link Observer} notified when the day selection panel is double-clicked
	 */
	public Observer<Integer> doubleClicked() {
		return doubleClicked.observer();
	}

	/**
	 * Requests input focus for this calendar panel
	 */
	public void requestInputFocus() {
		if (enabledState.is()) {
			dayLabels.get(dayValue.get()).requestFocusInWindow();
		}
	}

	/**
	 * @return a new {@link Builder} instance
	 */
	public static Builder builder() {
		return new DefaultBuilder();
	}

	/**
	 * @return the temporal types supported by this calendar panel
	 */
	public static Collection<Class<? extends Temporal>> supportedTypes() {
		return SUPPORTED_TYPES;
	}

	/**
	 * Provides access to the date.
	 */
	public interface CalendarDate extends Observable<LocalDate> {

		/**
		 * Sets the date to present in this calendar
		 * @param date the date to set
		 */
		void set(LocalDate date);
	}

	/**
	 * Provides access to the date and time.
	 */
	public interface CalendarDateTime extends Observable<LocalDateTime> {

		/**
		 * Sets the date and time to present in this calendar
		 * @param dateTime the date and time to set
		 */
		void set(LocalDateTime dateTime);
	}

	/**
	 * Provides access to the zoned date and time.
	 */
	public interface CalendarZonedDateTime extends Observable<ZonedDateTime> {

		/**
		 * Sets the date and time to present in this calendar
		 * @param zonedDateTime the date and time to set
		 */
		void set(ZonedDateTime zonedDateTime);
	}

	/**
	 * Builds a {@link CalendarPanel} instance.
	 */
	public interface Builder {

		/**
		 * Specifies the locale, controlling the start of week day and the full date display format.
		 * Note that setting the locale also sets {@link #firstDayOfWeek(DayOfWeek)}
		 * and {@link #minimalDaysInFirstWeek(int)} according to the given locale.
		 * @param locale the locale
		 * @return this builder instance
		 */
		Builder locale(Locale locale);

		/**
		 * Sets the first day of week, in case it should differ from the one specified by {@link #locale(Locale)}
		 * @param firstDayOfWeek the first day of week
		 * @return this builder instance
		 */
		Builder firstDayOfWeek(DayOfWeek firstDayOfWeek);

		/**
		 * @param minimalDaysInFirstWeek the minimal number of days in the first week of the year
		 * @return this builder instance
		 */
		Builder minimalDaysInFirstWeek(int minimalDaysInFirstWeek);

		/**
		 * Note that calling this method also sets {@link #includeTime(boolean)} to false.
		 * In case of a null value {@link LocalDate#now()} is used.
		 * @param value the initial value
		 * @return this builder instance
		 */
		Builder value(@Nullable LocalDate value);

		/**
		 * Note that calling this method also sets {@link #includeTime(boolean)} to true.
		 * In case of a null value {@link LocalDateTime#now()} is used.
		 * @param value the initial value
		 * @return this builder instance
		 */
		Builder value(@Nullable LocalDateTime value);

		/**
		 * Note that calling this method also sets {@link #includeTime(boolean)}
		 * and {@link #includeTimeZone(boolean)} to true.
		 * In case of a null value {@link ZonedDateTime#now()} is used.
		 * @param value the initial value
		 * @return this builder instance
		 */
		Builder value(@Nullable ZonedDateTime value);

		/**
		 * @param includeTime if true then time fields are included (hours, minutes)
		 * @return this builder instance
		 */
		Builder includeTime(boolean includeTime);

		/**
		 * @param includeTimeZone if true then a button for selecting a time zone is included
		 * @return this builder insteance
		 */
		Builder includeTimeZone(boolean includeTimeZone);

		/**
		 * @param includeTodayButton true if a 'Today' button for selecting the current date should be included
		 * @return this builder instance
		 */
		Builder includeTodayButton(boolean includeTodayButton);

		/**
		 * @param includeWeekNumbers true if week numbers should be displayed
		 * @return this builder instance
		 */
		Builder includeWeekNumbers(boolean includeWeekNumbers);

		/**
		 * @param controlKey the control key
		 * @param keyStroke the keyStroke to assign to the given control
		 * @return this builder instance
		 */
		Builder keyStroke(ControlKey<?> controlKey, KeyStroke keyStroke);

		/**
		 * @param enabled the state controlling the component enabled status
		 * @return this builder instance
		 */
		Builder enabled(ObservableState enabled);

		/**
		 * @return a new {@link CalendarPanel} based on this builder
		 */
		CalendarPanel build();
	}

	private final class DefaultCalendarDate implements CalendarDate {

		private final Value<LocalDate> value;

		private DefaultCalendarDate() {
			value = Value.nullable(createLocalDateTime().toLocalDate());
		}

		@Override
		public void set(LocalDate date) {
			dateTime.set(requireNonNull(date).atStartOfDay());
		}

		@Override
		public @Nullable LocalDate get() {
			return value.get();
		}

		@Override
		public Observer<LocalDate> observer() {
			return value.observer();
		}
	}

	private final class DefaultCalendarDateTime implements CalendarDateTime {

		private final Value<LocalDateTime> value;

		private DefaultCalendarDateTime() {
			value = Value.nullable(createLocalDateTime());
		}

		@Override
		public void set(LocalDateTime dateTime) {
			requireNonNull(dateTime);
			if (includeTime) {
				setYearMonthDayHourMinute(dateTime);
			}
			else {
				setYearMonthDay(dateTime.toLocalDate());
			}
		}

		@Override
		public @Nullable LocalDateTime get() {
			return value.get();
		}

		@Override
		public Observer<LocalDateTime> observer() {
			return value.observer();
		}
	}

	private final class DefaultCalendarZonedDateTime implements CalendarZonedDateTime {

		private final Value<ZonedDateTime> value;

		private DefaultCalendarZonedDateTime() {
			value = Value.nullable(createZonedDateTime());
		}

		@Override
		public void set(ZonedDateTime zonedDateTime) {
			requireNonNull(zonedDateTime);
			if (includeTime) {
				setYearMonthDayHourMinuteZoned(zonedDateTime);
			}
			else {
				setYearMonthDay(zonedDateTime.toLocalDate());
			}
		}

		@Override
		public ZonedDateTime get() {
			return value.getOrThrow();
		}

		@Override
		public Observer<ZonedDateTime> observer() {
			return value.observer();
		}

		private void setYearMonthDayHourMinuteZoned(ZonedDateTime zonedDateTime) {
			setYearMonthDayHourMinute(zonedDateTime.toLocalDateTime());
			zoneIdValue.set(zonedDateTime.getZone());
		}
	}

	private final class DefaultCalendarOffsetDateTime implements Observable<OffsetDateTime> {

		private final Value<OffsetDateTime> value;

		private DefaultCalendarOffsetDateTime() {
			value = Value.nullable(createZonedDateTime().toOffsetDateTime());
		}

		@Override
		public OffsetDateTime get() {
			return value.getOrThrow();
		}

		@Override
		public Observer<OffsetDateTime> observer() {
			return value.observer();
		}
	}

	private static final class DefaultBuilder implements Builder {

		private final ControlMap controlMap = controlMap(ControlKeys.class);

		private Locale locale = Locale.getDefault();
		private DayOfWeek firstDayOfWeek = WeekFields.of(locale).getFirstDayOfWeek();
		private int minimalDaysInFirstWeek = WeekFields.of(locale).getMinimalDaysInFirstWeek();
		private @Nullable ZonedDateTime value;
		private boolean includeTime = false;
		private boolean includeTimeZone = false;
		private boolean includeTodayButton = false;
		private boolean includeWeekNumbers = WEEK_NUMBERS.getOrThrow();
		private ObservableState enabled = State.state(true);

		@Override
		public Builder locale(Locale locale) {
			this.locale = requireNonNull(locale);
			WeekFields localeWeekFields = WeekFields.of(locale);
			this.minimalDaysInFirstWeek = localeWeekFields.getMinimalDaysInFirstWeek();
			this.firstDayOfWeek = localeWeekFields.getFirstDayOfWeek();
			return this;
		}

		@Override
		public Builder firstDayOfWeek(DayOfWeek firstDayOfWeek) {
			this.firstDayOfWeek = requireNonNull(firstDayOfWeek);
			return this;
		}

		@Override
		public Builder minimalDaysInFirstWeek(int minimalDaysInFirstWeek) {
			if (minimalDaysInFirstWeek < 1 || minimalDaysInFirstWeek > 7) {
				throw new IllegalArgumentException("Minimal number of days is invalid");
			}
			this.minimalDaysInFirstWeek = minimalDaysInFirstWeek;
			return this;
		}

		@Override
		public Builder value(@Nullable LocalDate value) {
			return value(value == null ? null : value.atStartOfDay());
		}

		@Override
		public Builder value(@Nullable LocalDateTime value) {
			this.value = value == null ? null : ZonedDateTime.of(value, ZoneId.systemDefault());
			return includeTime(true);
		}

		@Override
		public Builder value(@Nullable ZonedDateTime value) {
			this.value = value;
			return includeTime(true).includeTimeZone(true);
		}

		@Override
		public Builder includeTime(boolean includeTime) {
			this.includeTime = includeTime;
			return this;
		}

		@Override
		public Builder includeTimeZone(boolean includeTimeZone) {
			this.includeTimeZone = includeTimeZone;
			return this;
		}

		@Override
		public Builder includeTodayButton(boolean includeTodayButton) {
			this.includeTodayButton = includeTodayButton;
			return this;
		}

		@Override
		public Builder includeWeekNumbers(boolean includeWeekNumbers) {
			this.includeWeekNumbers = includeWeekNumbers;
			return this;
		}

		@Override
		public Builder keyStroke(ControlKey<?> controlKey, KeyStroke keyStroke) {
			controlMap.keyStroke(controlKey).set(keyStroke);
			return this;
		}

		@Override
		public Builder enabled(ObservableState enabled) {
			this.enabled = requireNonNull(enabled);
			return this;
		}

		@Override
		public CalendarPanel build() {
			return new CalendarPanel(this);
		}
	}

	void previousMonth() {
		subtractOne(ChronoUnit.MONTHS);
	}

	void nextMonth() {
		addOne(ChronoUnit.MONTHS);
	}

	void previousYear() {
		subtractOne(ChronoUnit.YEARS);
	}

	void nextYear() {
		addOne(ChronoUnit.YEARS);
	}

	void previousWeek() {
		subtractOne(ChronoUnit.WEEKS);
	}

	void nextWeek() {
		addOne(ChronoUnit.WEEKS);
	}

	void previousDay() {
		subtractOne(ChronoUnit.DAYS);
	}

	void nextDay() {
		addOne(ChronoUnit.DAYS);
	}

	void previousHour() {
		subtractOne(ChronoUnit.HOURS);
	}

	void nextHour() {
		addOne(ChronoUnit.HOURS);
	}

	void previousMinute() {
		subtractOne(ChronoUnit.MINUTES);
	}

	void nextMinute() {
		addOne(ChronoUnit.MINUTES);
	}

	private void subtractOne(ChronoUnit unit) {
		if (unit.isDateBased()) {
			setYearMonthDay(localDate().minus(1, unit));
		}
		else {
			setYearMonthDayHourMinute(dateTime.getOrThrow().minus(1, unit));
		}
	}

	private void addOne(ChronoUnit unit) {
		if (unit.isDateBased()) {
			setYearMonthDay(localDate().plus(1, unit));
		}
		else {
			setYearMonthDayHourMinute(dateTime.getOrThrow().plus(1, unit));
		}
	}

	private Map<Integer, DayLabel> createDayLabels() {
		return IntStream.rangeClosed(1, MAX_DAYS_IN_MONTH).boxed()
						.collect(toMap(identity(), DayLabel::new));
	}

	private void initializeUI() {
		setLayout(borderLayout());
		setBorder(emptyBorder());
		add(createNorthPanel(), BorderLayout.NORTH);
		add(createDayPanel(), BorderLayout.CENTER);
		layoutDayPanel();
	}

	private JPanel createNorthPanel() {
		return borderLayoutPanel()
						.north(borderLayoutPanel()
										.center(formattedDateLabel)
										.border(createTitledBorder("")))
						.center(flowLayoutPanel(FlowLayout.CENTER)
										.add(createYearMonthHourMinutePanel())
										.border(createTitledBorder("")))
						.build();
	}

	private JPanel createYearMonthHourMinutePanel() {
		JSpinner yearSpinner = createYearSpinner();
		JSpinner monthSpinner = createMonthSpinner(yearSpinner);
		JSpinner hourSpinner = createHourSpinner();
		JSpinner minuteSpinner = createMinuteSpinner();

		FlexibleGridLayoutPanelBuilder yearMonthHourMinutePanel = flexibleGridLayoutPanel(1, 0)
						.add(monthSpinner)
						.add(yearSpinner);
		if (includeTime) {
			yearMonthHourMinutePanel.addAll(hourSpinner, new JLabel(":", SwingConstants.CENTER), minuteSpinner);
			if (includeTimeZone) {
				yearMonthHourMinutePanel.add(createTimeZoneButton());
			}
		}
		if (includeTodayButton) {
			yearMonthHourMinutePanel.add(createSelectTodayButton());
		}

		return yearMonthHourMinutePanel.build();
	}

	private JButton createSelectTodayButton() {
		return button()
						.control(command(this::selectToday))
						.text(MESSAGES.getString("today"))
						.mnemonic(MESSAGES.getString("today_mnemonic").charAt(0))
						.enabled(todaySelected.not())
						.build();
	}

	private JPanel createDayPanel() {
		JPanel dayPanel = borderLayoutPanel()
						.north(createDayHeaderPanel())
						.center(dayGridPanel)
						.border(createTitledBorder(""))
						.build();
		if (!includeWeekNumbers) {
			return dayPanel;
		}

		weekNumberPanel = createWeekNumberPanel();

		JPanel weekPanel = borderLayoutPanel()
						.north(gridLayoutPanel(1, 1)
										.add(label(MESSAGES.getString("week"))
														.horizontalAlignment(SwingConstants.CENTER)
														.border(emptyBorder())
														.enabled(enabledState)))
						.center(weekNumberPanel)
						.border(createTitledBorder(""))
						.build();

		return borderLayoutPanel()
						.west(weekPanel)
						.center(dayPanel)
						.build();
	}

	private JPanel createDayHeaderPanel() {
		GridLayoutPanelBuilder panelBuilder = gridLayoutPanel(1, DAYS_IN_WEEK);
		dayColumns.forEach(dayOfWeek -> panelBuilder.add(createDayLabel(dayOfWeek)));

		return panelBuilder.build();
	}

	private JLabel createDayLabel(DayOfWeek dayOfWeek) {
		return label(dayOfWeek.getDisplayName(TextStyle.SHORT, selectedLocale))
						.horizontalAlignment(SwingConstants.CENTER)
						.border(emptyBorder())
						.enabled(enabledState)
						.build();
	}

	private JPanel createWeekNumberPanel() {
		JPanel weekPanel = new JPanel(new GridLayout(DAY_GRID_ROWS, 1));
		addWeekNumbers(weekPanel);

		return weekPanel;
	}

	private LocalDate getFirstVisibleDate() {
		LocalDate firstOfMonth = LocalDate.of(yearValue.getOrThrow(), monthValue.getOrThrow(), 1);

		return firstOfMonth.minusDays(dayColumns.indexOf(firstOfMonth.getDayOfWeek()));
	}

	private void updateWeekNumbers() {
		if (weekNumberPanel != null) {
			weekNumberPanel.removeAll();
			addWeekNumbers(weekNumberPanel);
		}
	}

	private void addWeekNumbers(JPanel panel) {
		WeekFields weekFields = WeekFields.of(firstDayOfWeek, minimalDaysInFirstWeek);
		LocalDate firstVisibleDate = getFirstVisibleDate();
		for (int row = 0; row < DAY_GRID_ROWS; row++) {
			LocalDate weekDate = firstVisibleDate.plusWeeks(row);
			int weekNumber = weekDate.get(weekFields.weekOfWeekBasedYear());
			panel.add(label(String.valueOf(weekNumber))
							.horizontalAlignment(SwingConstants.CENTER)
							.border(createEtchedBorder())
							.enabled(false)
							.build());
		}
	}

	private void layoutDayPanel() {
		getCurrentKeyboardFocusManager().clearFocusOwner();
		dayGridPanel.removeAll();

		updateWeekNumbers();

		DayOfWeek dayOfWeek = date.getOrThrow().withDayOfMonth(1).getDayOfWeek();
		int dayOfWeekColumn = dayColumns.indexOf(dayOfWeek);
		YearMonth previousMonth = YearMonth.of(yearValue.getOrThrow(), monthValue.getOrThrow()).minusMonths(1);
		int previousMonthLength = previousMonth.lengthOfMonth();
		for (int i = dayOfWeekColumn - 1; i >= 0; i--) {
			dayGridPanel.add(new FillerDayLabel(LocalDate.of(previousMonth.getYear(), previousMonth.getMonth(), previousMonthLength - i)));
		}
		int counter = dayOfWeekColumn;
		YearMonth yearMonth = YearMonth.of(yearValue.getOrThrow(), monthValue.getOrThrow());
		for (int dayOfMonth = 1; dayOfMonth <= yearMonth.lengthOfMonth(); dayOfMonth++) {
			dayGridPanel.add(dayLabels.get(dayOfMonth));
			counter++;
		}
		YearMonth nextMonth = yearMonth.plusMonths(1);
		int nextMonthDay = 1;
		while (counter++ < DAY_GRID_CELLS) {
			dayGridPanel.add(new FillerDayLabel(LocalDate.of(nextMonth.getYear(), nextMonth.getMonth(), nextMonthDay++)));
		}
		requestInputFocus();
		validate();
		repaint();
	}

	private void setYearMonthDay(LocalDate localDate) {
		yearValue.set(localDate.getYear());
		monthValue.set(localDate.getMonth());
		dayValue.set(localDate.getDayOfMonth());
	}

	private void setYearMonthDayHourMinute(LocalDateTime localDateTime) {
		setYearMonthDay(localDateTime.toLocalDate());
		hourValue.set(localDateTime.getHour());
		minuteValue.set(localDateTime.getMinute());
	}

	private LocalDateTime createLocalDateTime() {
		return LocalDateTime.of(yearValue.getOrThrow(), monthValue.getOrThrow(), dayValue.getOrThrow(), hourValue.getOrThrow(), minuteValue.getOrThrow());
	}

	private ZonedDateTime createZonedDateTime() {
		return ZonedDateTime.of(createLocalDateTime(), zoneIdValue.getOrThrow());
	}

	private void updateDateTime() {
		//prevent illegal day values
		YearMonth yearMonth = YearMonth.of(yearValue.getOrThrow(), monthValue.getOrThrow());
		dayValue.update(day -> day > yearMonth.lengthOfMonth() ? yearMonth.lengthOfMonth() : day);
		LocalDateTime localDateTime = createLocalDateTime();
		date.value.set(localDateTime.toLocalDate());
		dateTime.value.set(localDateTime);
		ZonedDateTime zoned = createZonedDateTime();
		zonedDateTime.value.set(zoned);
		offsetDateTime.value.set(zoned.toOffsetDateTime());
		todaySelected.set(todaySelected());
		updateDayLabelBorders();
		requestInputFocus();
		invokeLater(this::updateFormattedDate);
	}

	private void updateDayLabelBorders() {
		dayLabels.values().forEach(DayLabel::updateBorder);
	}

	private boolean todaySelected() {
		return localDate().equals(LocalDate.now());
	}

	private LocalDate localDate() {
		return LocalDate.of(yearValue.getOrThrow(), monthValue.getOrThrow(), dayValue.getOrThrow());
	}

	private void selectToday() {
		LocalDate now = LocalDate.now();
		dateTime.set(dateTime.getOrThrow()
						.withYear(now.getYear())
						.withMonth(now.getMonthValue())
						.withDayOfMonth(now.getDayOfMonth()));
		requestInputFocus();
	}

	private void updateFormattedDate() {
		ZonedDateTime zoned = zonedDateTime.getOrThrow();
		formattedDateLabel.setText(dateFormatter.format(zoned) + (includeTime ? ", " + timeFormatter.format(zoned) : ""));
	}

	private void createControls() {
		controlMap.control(PREVIOUS_YEAR).set(Control.builder()
						.command(this::previousYear)
						.enabled(enabledState)
						.build());
		controlMap.control(NEXT_YEAR).set(Control.builder()
						.command(this::nextYear)
						.enabled(enabledState)
						.build());
		controlMap.control(PREVIOUS_MONTH).set(Control.builder()
						.command(this::previousMonth)
						.enabled(enabledState)
						.build());
		controlMap.control(NEXT_MONTH).set(Control.builder()
						.command(this::nextMonth)
						.enabled(enabledState)
						.build());
		controlMap.control(PREVIOUS_WEEK).set(Control.builder()
						.command(this::previousWeek)
						.enabled(enabledState)
						.build());
		controlMap.control(NEXT_WEEK).set(Control.builder()
						.command(this::nextWeek)
						.enabled(enabledState)
						.build());
		controlMap.control(PREVIOUS_DAY).set(Control.builder()
						.command(this::previousDay)
						.enabled(enabledState)
						.build());
		controlMap.control(NEXT_DAY).set(Control.builder()
						.command(this::nextDay)
						.enabled(enabledState)
						.build());
		controlMap.control(PREVIOUS_HOUR).set(Control.builder()
						.command(this::previousHour)
						.enabled(enabledState)
						.build());
		controlMap.control(NEXT_HOUR).set(Control.builder()
						.command(this::nextHour)
						.enabled(enabledState)
						.build());
		controlMap.control(PREVIOUS_MINUTE).set(Control.builder()
						.command(this::previousMinute)
						.enabled(enabledState)
						.build());
		controlMap.control(NEXT_MINUTE).set(Control.builder()
						.command(this::nextMinute)
						.enabled(enabledState)
						.build());
	}

	private void addKeyEvents() {
		controlMap.keyEvent(PREVIOUS_YEAR).ifPresent(keyEvent ->
						keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.enable(this));
		controlMap.keyEvent(NEXT_YEAR).ifPresent(keyEvent ->
						keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.enable(this));
		controlMap.keyEvent(PREVIOUS_MONTH).ifPresent(keyEvent ->
						keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.enable(this));
		controlMap.keyEvent(NEXT_MONTH).ifPresent(keyEvent ->
						keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.enable(this));
		controlMap.keyEvent(PREVIOUS_WEEK).ifPresent(keyEvent ->
						keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.enable(this));
		controlMap.keyEvent(NEXT_WEEK).ifPresent(keyEvent ->
						keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.enable(this));
		controlMap.keyEvent(PREVIOUS_DAY).ifPresent(keyEvent ->
						keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.enable(this));
		controlMap.keyEvent(NEXT_DAY).ifPresent(keyEvent ->
						keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.enable(this));
		if (includeTime) {
			controlMap.keyEvent(PREVIOUS_HOUR).ifPresent(keyEvent ->
							keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
											.enable(this));
			controlMap.keyEvent(NEXT_HOUR).ifPresent(keyEvent ->
							keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
											.enable(this));
			controlMap.keyEvent(PREVIOUS_MINUTE).ifPresent(keyEvent ->
							keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
											.enable(this));
			controlMap.keyEvent(NEXT_MINUTE).ifPresent(keyEvent ->
							keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
											.enable(this));
		}
	}

	private JSpinner createYearSpinner() {
		return integerSpinner()
						.model(new SpinnerNumberModel(0, -9999, 9999, 1))
						.link(yearValue)
						.horizontalAlignment(SwingConstants.CENTER)
						.columns(YEAR_COLUMNS)
						.editable(false)
						.groupingUsed(false)
						.enabled(enabledState)
						.onBuild(CalendarPanel::removeCtrlLeftRightArrowKeyEvents)
						.build();
	}

	private JSpinner createMonthSpinner(JSpinner yearSpinner) {
		List<Item<Month>> monthItems = createMonthItems();
		JSpinner monthSpinner = Components.<Month>itemSpinner()
						.model(new SpinnerListModel(monthItems))
						.link(monthValue)
						.horizontalAlignment(SwingConstants.CENTER)
						.editable(false)
						.enabled(enabledState)
						.onBuild(CalendarPanel::removeCtrlLeftRightArrowKeyEvents)
						.build();
		JFormattedTextField monthTextField = ((JSpinner.DefaultEditor) monthSpinner.getEditor()).getTextField();
		monthTextField.setFont(((JSpinner.DefaultEditor) yearSpinner.getEditor()).getTextField().getFont());
		monthItems.stream()
						.mapToInt(item -> item.caption().length())
						.max()
						.ifPresent(monthTextField::setColumns);

		return monthSpinner;
	}

	private List<Item<Month>> createMonthItems() {
		return Arrays.stream(Month.values())
						.map(month -> Item.item(month, month.getDisplayName(TextStyle.SHORT, selectedLocale)))
						.collect(toList());
	}

	private JSpinner createHourSpinner() {
		return integerSpinner()
						.model(new SpinnerNumberModel(0, 0, 23, 1))
						.link(hourValue)
						.horizontalAlignment(SwingConstants.CENTER)
						.columns(TIME_COLUMNS)
						.editable(false)
						.decimalFormatPattern("00")
						.enabled(enabledState)
						.onBuild(CalendarPanel::removeCtrlLeftRightArrowKeyEvents)
						.build();
	}

	private JSpinner createMinuteSpinner() {
		return integerSpinner()
						.model(new SpinnerNumberModel(0, 0, 59, 1))
						.link(minuteValue)
						.horizontalAlignment(SwingConstants.CENTER)
						.columns(TIME_COLUMNS)
						.editable(false)
						.decimalFormatPattern("00")
						.enabled(enabledState)
						.onBuild(CalendarPanel::removeCtrlLeftRightArrowKeyEvents)
						.build();
	}

	private JButton createTimeZoneButton() {
		return Components.button()
						.control(Control.builder()
										.command(this::selectTimeZone)
										.caption("Z")
										.mnemonic('Z'))
						.focusable(false)
						.build();
	}

	private void selectTimeZone() {
		Dialogs.select()
						.comboBox(getAvailableZoneIds().stream()
										.map(ZoneId::of)
										.sorted(comparing(zoneId -> zoneId.getDisplayName(TextStyle.FULL, selectedLocale)))
										.collect(toList()))
						.defaultSelection(zoneIdValue.getOrThrow())
						.owner(this)
						.select()
						.ifPresent(zoneIdValue::set);
	}

	private void enabledChanged(boolean enabled) {
		dayLabels.values().forEach(label -> label.setEnabled(enabled));
	}

	private static JSpinner removeCtrlLeftRightArrowKeyEvents(JSpinner spinner) {
		InputMap inputMap = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().getInputMap(WHEN_FOCUSED);
		//so it doesn't interfere with keyboard navigation when it has focus
		inputMap.put(keyStroke(VK_LEFT, MENU_SHORTCUT_MASK), "none");
		inputMap.put(keyStroke(VK_RIGHT, MENU_SHORTCUT_MASK), "none");
		inputMap.put(keyStroke(VK_LEFT, SHIFT_DOWN_MASK), "none");
		inputMap.put(keyStroke(VK_RIGHT, SHIFT_DOWN_MASK), "none");

		return spinner;
	}

	private final class DayLabel extends JLabel {

		private final int day;

		private DayLabel(Integer day) {
			super(day.toString());
			this.day = day.intValue();
			setHorizontalAlignment(CENTER);
			setFocusable(true);
			setEnabled(enabledState.is());
			addMouseListener(dayMouseListener);
		}

		@Override
		public void updateUI() {
			super.updateUI();
			if (dayValue != null) {
				updateBorder();
			}
		}

		@Override
		public void setEnabled(boolean enabled) {
			super.setEnabled(enabled);
			updateBorder();
		}

		private void updateBorder() {
			Color foreground = isEnabled() ? getForeground() : UIManager.getColor("Label.disabledForeground");
			setBorder(dayValue.is(day) ? createEtchedBorder(foreground, foreground) : createEtchedBorder());
		}
	}

	private final class FillerDayLabel extends JLabel {

		private final LocalDate localDate;

		private FillerDayLabel(LocalDate localDate) {
			super(String.valueOf(localDate.getDayOfMonth()));
			this.localDate = localDate;
			setEnabled(false);
			setHorizontalAlignment(SwingConstants.CENTER);
			setBorder(createEtchedBorder());
			addMouseListener(dayMouseListener);
		}
	}

	private final class DayMouseListener extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent e) {
			if (enabledState.is()) {
				JComponent component = (JComponent) e.getSource();
				if (component instanceof DayLabel) {
					DayLabel label = (DayLabel) component;
					dayValue.set(label.day);
					if (e.getClickCount() == 2) {
						doubleClicked.accept(label.day);
					}
				}
				else if (component instanceof FillerDayLabel) {
					FillerDayLabel label = (FillerDayLabel) component;
					yearValue.set(label.localDate.getYear());
					monthValue.set(label.localDate.getMonth());
					dayValue.set(label.localDate.getDayOfMonth());
				}
			}
		}
	}

	private final class UpdateDateTime implements Runnable {

		@Override
		public void run() {
			updateDateTime();
		}
	}

	private final class LayoutDayPanel implements Runnable {

		@Override
		public void run() {
			if (isEventDispatchThread()) {
				layoutDayPanel();
			}
			else {
				invokeLater(CalendarPanel.this::layoutDayPanel);
			}
		}
	}
}
