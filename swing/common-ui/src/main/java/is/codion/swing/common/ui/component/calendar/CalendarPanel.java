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
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.calendar;

import is.codion.common.item.Item;
import is.codion.common.resource.MessageBundle;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;
import is.codion.swing.common.ui.component.panel.PanelBuilder;
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.ControlKey;
import is.codion.swing.common.ui.control.ControlMap;

import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.WeekFields;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.border.Borders.emptyBorder;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.component.calendar.CalendarPanel.ControlKeys.*;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.control.ControlMap.controlMap;
import static is.codion.swing.common.ui.key.KeyEvents.keyStroke;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.InputEvent.SHIFT_DOWN_MASK;
import static java.awt.event.KeyEvent.*;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
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
 * <li>Previous/next year: CTRL + left/right arrow or down/up arrow.
 * <li>Previous/next month: SHIFT + left/right arrow or down/up arrow.
 * <li>Previous/next week: up/down arrow.
 * <li>Previous/next day: left/right arrow.
 * <li>Previous/next hour: SHIFT-ALT + left/right arrow or down/up arrow.
 * <li>Previous/next minute: CTRL-ALT + left/right arrow or down/up arrow.
 * @see #builder()
 */
public final class CalendarPanel extends JPanel {

	private static final MessageBundle MESSAGES =
					messageBundle(CalendarPanel.class, getBundle(CalendarPanel.class.getName()));

	/**
	 * The available controls.
	 */
	public static final class ControlKeys {

		/**
		 * Select the previous year.<br>
		 * Default key stroke: CTRL-DOWN ARROW
		 */
		public static final ControlKey<CommandControl> PREVIOUS_YEAR = CommandControl.key("previousYear", keyStroke(VK_DOWN, CTRL_DOWN_MASK));
		/**
		 * Select the next year.<br>
		 * Default key stroke: CTRL-UP ARROW
		 */
		public static final ControlKey<CommandControl> NEXT_YEAR = CommandControl.key("nextYear", keyStroke(VK_UP, CTRL_DOWN_MASK));
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
		public static final ControlKey<CommandControl> PREVIOUS_MINUTE = CommandControl.key("previousMinute", keyStroke(VK_DOWN, CTRL_DOWN_MASK | ALT_DOWN_MASK));
		/**
		 * Select the next minute.<br>
		 * Default key stroke: CTRL-ALT-UP ARROW
		 */
		public static final ControlKey<CommandControl> NEXT_MINUTE = CommandControl.key("nextMinute", keyStroke(VK_UP, CTRL_DOWN_MASK | ALT_DOWN_MASK));

		private ControlKeys() {}
	}

	private static final Set<Class<? extends Temporal>> SUPPORTED_TYPES =
					unmodifiableSet(new HashSet<>(asList(LocalDate.class, LocalDateTime.class)));

	private static final int YEAR_COLUMNS = 4;
	private static final int TIME_COLUMNS = 2;
	private static final int DAYS_IN_WEEK = 7;
	private static final int MAX_DAYS_IN_MONTH = 31;
	private static final int MAX_DAY_FILLERS = 14;
	private static final int DAY_GRID_ROWS = 6;
	private static final int DAY_GRID_CELLS = 42;

	private final Locale selectedLocale;
	private final DayOfWeek firstDayOfWeek;
	private final DateTimeFormatter dateFormatter;
	private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

	private final Value<LocalDate> localDateValue;
	private final Value<LocalDateTime> localDateTimeValue;

	private final Value<Integer> yearValue;
	private final Value<Month> monthValue;
	private final Value<Integer> dayValue;
	private final Value<Integer> hourValue;
	private final Value<Integer> minuteValue;
	private final State todaySelected;

	private final ControlMap controlMap;
	private final List<DayOfWeek> dayColumns;
	private final Map<Integer, DayLabel> dayLabels;
	private final JPanel dayGridPanel;
	private final List<JLabel> paddingLabels;
	private final JLabel formattedDateLabel;
	private final boolean includeTime;
	private final boolean includeTodayButton;

	CalendarPanel(DefaultBuilder builder) {
		this.includeTime = builder.includeTime;
		this.includeTodayButton = builder.includeTodayButton;
		this.controlMap = builder.controlMap;
		this.selectedLocale = builder.locale;
		this.firstDayOfWeek = builder.firstDayOfWeek;
		this.dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(selectedLocale);
		LocalDateTime dateTime = builder.initialValue == null ? LocalDateTime.now() : builder.initialValue;
		yearValue = Value.builder()
						.nonNull(dateTime.getYear())
						.listener(this::updateDateTime)
						.listener(new LayoutDayPanelListener())
						.build();
		monthValue = Value.builder()
						.nonNull(dateTime.getMonth())
						.listener(this::updateDateTime)
						.listener(new LayoutDayPanelListener())
						.build();
		dayValue = Value.builder()
						.nonNull(dateTime.getDayOfMonth())
						.listener(this::updateDateTime)
						.build();
		if (includeTime) {
			hourValue = Value.builder()
							.nonNull(dateTime.getHour())
							.listener(this::updateDateTime)
							.build();
			minuteValue = Value.builder()
							.nonNull(dateTime.getMinute())
							.listener(this::updateDateTime)
							.build();
		}
		else {
			hourValue = Value.builder()
							.nonNull(0)
							.build();
			minuteValue = Value.builder()
							.nonNull(0)
							.build();
		}
		localDateValue = Value.value(createLocalDateTime().toLocalDate());
		localDateTimeValue = Value.value(createLocalDateTime());
		todaySelected = State.state(todaySelected());
		dayColumns = IntStream.range(0, DAYS_IN_WEEK)
						.mapToObj(firstDayOfWeek::plus)
						.collect(toList());
		dayLabels = createDayLabels();
		paddingLabels = IntStream.range(0, MAX_DAY_FILLERS).mapToObj(counter -> new JLabel()).collect(toList());
		dayGridPanel = new JPanel(new GridLayout(DAY_GRID_ROWS, DAYS_IN_WEEK));
		formattedDateLabel = new JLabel("", SwingConstants.CENTER);
		formattedDateLabel.setBorder(emptyBorder());
		initializeUI();
		createControls();
		addKeyEvents();
		updateFormattedDate();
		updateDayLabelBorders();
	}

	/**
	 * Sets the date to present in this calendar
	 * @param date the date to set
	 */
	public void setLocalDate(LocalDate date) {
		setLocalDateTime(requireNonNull(date).atStartOfDay());
	}

	/**
	 * @return the date currently displayed in this calendar
	 */
	public LocalDate getLocalDate() {
		return LocalDate.of(yearValue.get(), monthValue.get(), dayValue.get());
	}

	/**
	 * Sets the date/time to present in this calendar.
	 * @param dateTime the date/time to set
	 */
	public void setLocalDateTime(LocalDateTime dateTime) {
		requireNonNull(dateTime);
		if (includeTime) {
			setYearMonthDayHourMinute(dateTime);
		}
		else {
			setYearMonthDay(dateTime.toLocalDate());
		}
	}

	/**
	 * @return the date/time currently displayed in this calendar
	 */
	public LocalDateTime getLocalDateTime() {
		return localDateTimeValue.get();
	}

	/**
	 * Requests input focus for this calendar panel
	 */
	public void requestInputFocus() {
		dayLabels.get(dayValue.get()).requestFocusInWindow();
	}

	/**
	 * @return an observer notified each time the date changes
	 */
	public ValueObserver<LocalDate> localDateValue() {
		return localDateValue.observer();
	}

	/**
	 * @return an observer notified each time the date or time changes
	 */
	public ValueObserver<LocalDateTime> localDateTimeValue() {
		return localDateTimeValue.observer();
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
	 * Builds a {@link CalendarPanel} instance.
	 */
	public interface Builder {

		/**
		 * Specifies the locale, controlling the start of week day and the full date display format.
		 * Note that setting the locale also sets {@link #firstDayOfWeek(DayOfWeek)} according to the given locale.
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
		 * Note that calling this method also sets {@link #includeTime(boolean)} to false.
		 * In case of a null value {@link LocalDate#now()} is used.
		 * @param initialValue the initial value
		 * @return this builder instance
		 */
		Builder initialValue(LocalDate initialValue);

		/**
		 * Note that calling this method also sets {@link #includeTime(boolean)} to true.
		 * In case of a null value {@link LocalDateTime#now()} is used.
		 * @param initialValue the initial value
		 * @return this builder instance
		 */
		Builder initialValue(LocalDateTime initialValue);

		/**
		 * @param includeTime if true then time fields are included (hours, minutes)
		 * @return this builder instance
		 */
		Builder includeTime(boolean includeTime);

		/**
		 * @param includeTodayButton true if a 'Today' button for selecting the current date should be included
		 * @return this builder instance
		 */
		Builder includeTodayButton(boolean includeTodayButton);

		/**
		 * @param controlKey the control key
		 * @param keyStroke the keyStroke to assign to the given control
		 * @return this builder instance
		 */
		Builder keyStroke(ControlKey<?> controlKey, KeyStroke keyStroke);

		/**
		 * @return a new {@link CalendarPanel} based on this builder
		 */
		CalendarPanel build();
	}

	private static final class DefaultBuilder implements Builder {

		private final ControlMap controlMap = controlMap(ControlKeys.class);

		private Locale locale = Locale.getDefault();
		private DayOfWeek firstDayOfWeek = WeekFields.of(locale).getFirstDayOfWeek();
		private LocalDateTime initialValue;
		private boolean includeTime = false;
		private boolean includeTodayButton = false;

		@Override
		public Builder locale(Locale locale) {
			this.locale = requireNonNull(locale);
			return firstDayOfWeek(WeekFields.of(locale).getFirstDayOfWeek());
		}

		@Override
		public Builder firstDayOfWeek(DayOfWeek firstDayOfWeek) {
			this.firstDayOfWeek = requireNonNull(firstDayOfWeek);
			return this;
		}

		@Override
		public Builder initialValue(LocalDate initialValue) {
			this.initialValue = initialValue == null ? LocalDate.now().atStartOfDay() : initialValue.atStartOfDay();
			return includeTime(false);
		}

		@Override
		public Builder initialValue(LocalDateTime initialValue) {
			this.initialValue = initialValue == null ? LocalDateTime.now() : initialValue;
			return includeTime(true);
		}

		@Override
		public Builder includeTime(boolean includeTime) {
			this.includeTime = includeTime;
			return this;
		}

		@Override
		public Builder includeTodayButton(boolean includeTodayButton) {
			this.includeTodayButton = includeTodayButton;
			return this;
		}

		@Override
		public Builder keyStroke(ControlKey<?> controlKey, KeyStroke keyStroke) {
			controlMap.keyStroke(controlKey).set(keyStroke);
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
			setYearMonthDay(getLocalDate().minus(1, unit));
		}
		else {
			setYearMonthDayHourMinute(getLocalDateTime().minus(1, unit));
		}
	}

	private void addOne(ChronoUnit unit) {
		if (unit.isDateBased()) {
			setYearMonthDay(getLocalDate().plus(1, unit));
		}
		else {
			setYearMonthDayHourMinute(getLocalDateTime().plus(1, unit));
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
						.northComponent(borderLayoutPanel()
										.centerComponent(formattedDateLabel)
										.border(createTitledBorder(""))
										.build())
						.centerComponent(flowLayoutPanel(FlowLayout.CENTER)
										.add(createYearMonthHourMinutePanel())
										.border(createTitledBorder(""))
										.build())
						.build();
	}

	private JPanel createYearMonthHourMinutePanel() {
		JSpinner yearSpinner = createYearSpinner();
		JSpinner monthSpinner = createMonthSpinner(yearSpinner);
		JSpinner hourSpinner = createHourSpinner();
		JSpinner minuteSpinner = createMinuteSpinner();

		PanelBuilder yearMonthHourMinutePanel = flexibleGridLayoutPanel(1, 0)
						.add(monthSpinner)
						.add(yearSpinner);
		if (includeTime) {
			yearMonthHourMinutePanel.addAll(hourSpinner, new JLabel(":", SwingConstants.CENTER), minuteSpinner);
		}
		if (includeTodayButton) {
			yearMonthHourMinutePanel.add(createSelectTodayButton());
		}

		return yearMonthHourMinutePanel.build();
	}

	private JButton createSelectTodayButton() {
		return button(command(this::selectToday))
						.text(MESSAGES.getString("today"))
						.mnemonic(MESSAGES.getString("today_mnemonic").charAt(0))
						.enabled(todaySelected.not())
						.build();
	}

	private JPanel createDayPanel() {
		return borderLayoutPanel()
						.northComponent(createDayHeaderPanel())
						.centerComponent(dayGridPanel)
						.border(createTitledBorder(""))
						.build();
	}

	private JPanel createDayHeaderPanel() {
		PanelBuilder panelBuilder = gridLayoutPanel(1, DAYS_IN_WEEK);
		dayColumns.forEach(dayOfWeek -> panelBuilder.add(createDayLabel(dayOfWeek)));

		return panelBuilder.build();
	}

	private JLabel createDayLabel(DayOfWeek dayOfWeek) {
		return label(dayOfWeek.getDisplayName(TextStyle.SHORT, selectedLocale))
						.horizontalAlignment(SwingConstants.CENTER)
						.border(emptyBorder())
						.build();
	}

	private void layoutDayPanel() {
		getCurrentKeyboardFocusManager().clearFocusOwner();
		dayGridPanel.removeAll();
		DayOfWeek dayOfWeek = localDateValue.get().withDayOfMonth(1).getDayOfWeek();
		Iterator<JLabel> paddingIterator = paddingLabels.iterator();
		int dayOfWeekColumn = dayColumns.indexOf(dayOfWeek);
		for (int i = 0; i < dayOfWeekColumn; i++) {
			dayGridPanel.add(paddingIterator.next());
		}
		int counter = dayOfWeekColumn + 1;
		YearMonth yearMonth = YearMonth.of(yearValue.get(), monthValue.get());
		for (int dayOfMonth = 1; dayOfMonth <= yearMonth.lengthOfMonth(); dayOfMonth++) {
			dayGridPanel.add(dayLabels.get(dayOfMonth));
			counter++;
		}
		while (counter++ < DAY_GRID_CELLS) {
			dayGridPanel.add(paddingIterator.next());
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
		return LocalDateTime.of(yearValue.get(), monthValue.get(), dayValue.get(), hourValue.get(), minuteValue.get());
	}

	private void updateDateTime() {
		//prevent illegal day values
		YearMonth yearMonth = YearMonth.of(yearValue.get(), monthValue.get());
		dayValue.map(day -> day > yearMonth.lengthOfMonth() ? yearMonth.lengthOfMonth() : day);
		LocalDateTime localDateTime = createLocalDateTime();
		localDateValue.set(localDateTime.toLocalDate());
		localDateTimeValue.set(localDateTime);
		todaySelected.set(todaySelected());
		updateDayLabelBorders();
		requestInputFocus();
		invokeLater(this::updateFormattedDate);
	}

	private void updateDayLabelBorders() {
		dayLabels.values().forEach(DayLabel::updateBorder);
	}

	private boolean todaySelected() {
		return getLocalDate().equals(LocalDate.now());
	}

	private void selectToday() {
		LocalDate now = LocalDate.now();
		setLocalDateTime(getLocalDateTime().withYear(now.getYear()).withMonth(now.getMonthValue()).withDayOfMonth(now.getDayOfMonth()));
		requestInputFocus();
	}

	private void updateFormattedDate() {
		formattedDateLabel.setText(dateFormatter.format(getLocalDateTime()) + (includeTime ? ", " + timeFormatter.format(getLocalDateTime()) : ""));
	}

	private void createControls() {
		controlMap.control(PREVIOUS_YEAR).set(command(this::previousYear));
		controlMap.control(NEXT_YEAR).set(command(this::nextYear));
		controlMap.control(PREVIOUS_MONTH).set(command(this::previousMonth));
		controlMap.control(NEXT_MONTH).set(command(this::nextMonth));
		controlMap.control(PREVIOUS_WEEK).set(command(this::previousWeek));
		controlMap.control(NEXT_WEEK).set(command(this::nextWeek));
		controlMap.control(PREVIOUS_DAY).set(command(this::previousDay));
		controlMap.control(NEXT_DAY).set(command(this::nextDay));
		controlMap.control(PREVIOUS_HOUR).set(command(this::previousHour));
		controlMap.control(NEXT_HOUR).set(command(this::nextHour));
		controlMap.control(PREVIOUS_MINUTE).set(command(this::previousMinute));
		controlMap.control(NEXT_MINUTE).set(command(this::nextMinute));
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
		return integerSpinner(new SpinnerNumberModel(0, -9999, 9999, 1), yearValue)
						.horizontalAlignment(SwingConstants.CENTER)
						.columns(YEAR_COLUMNS)
						.editable(false)
						.groupingUsed(false)
						.onBuild(CalendarPanel::removeCtrlLeftRightArrowKeyEvents)
						.build();
	}

	private JSpinner createMonthSpinner(JSpinner yearSpinner) {
		List<Item<Month>> monthItems = createMonthItems();
		JSpinner monthSpinner = itemSpinner(new SpinnerListModel(monthItems), monthValue)
						.horizontalAlignment(SwingConstants.CENTER)
						.editable(false)
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
		return integerSpinner(new SpinnerNumberModel(0, 0, 23, 1), hourValue)
						.horizontalAlignment(SwingConstants.CENTER)
						.columns(TIME_COLUMNS)
						.editable(false)
						.decimalFormatPattern("00")
						.onBuild(CalendarPanel::removeCtrlLeftRightArrowKeyEvents)
						.build();
	}

	private JSpinner createMinuteSpinner() {
		return integerSpinner(new SpinnerNumberModel(0, 0, 59, 1), minuteValue)
						.horizontalAlignment(SwingConstants.CENTER)
						.columns(TIME_COLUMNS)
						.editable(false)
						.decimalFormatPattern("00")
						.onBuild(CalendarPanel::removeCtrlLeftRightArrowKeyEvents)
						.build();
	}

	private static JSpinner removeCtrlLeftRightArrowKeyEvents(JSpinner spinner) {
		InputMap inputMap = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().getInputMap(WHEN_FOCUSED);
		//so it doesn't interfere with keyboard navigation when it has focus
		inputMap.put(keyStroke(VK_LEFT, CTRL_DOWN_MASK), "none");
		inputMap.put(keyStroke(VK_RIGHT, CTRL_DOWN_MASK), "none");
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
			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					dayValue.set(day);
				}
			});
		}

		@Override
		public void updateUI() {
			super.updateUI();
			if (dayValue != null) {
				updateBorder();
			}
		}

		private void updateBorder() {
			setBorder(dayValue.isEqualTo(day) ? createEtchedBorder(getForeground(), getForeground()) : createEtchedBorder());
		}
	}

	private final class LayoutDayPanelListener implements Runnable {

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
