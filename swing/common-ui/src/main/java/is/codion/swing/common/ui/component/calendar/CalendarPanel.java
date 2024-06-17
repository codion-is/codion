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
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ControlKey;
import is.codion.swing.common.ui.control.ControlKeyStrokes;
import is.codion.swing.common.ui.key.KeyEvents;

import javax.swing.FocusManager;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.border.Borders.emptyBorder;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.component.calendar.CalendarPanel.ControlKeys.*;
import static is.codion.swing.common.ui.control.Control.commandControl;
import static is.codion.swing.common.ui.control.ControlKeyStrokes.controlKeyStrokes;
import static is.codion.swing.common.ui.control.ControlKeyStrokes.keyStroke;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.InputEvent.SHIFT_DOWN_MASK;
import static java.awt.event.KeyEvent.*;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;
import static javax.swing.BorderFactory.createTitledBorder;

/**
 * A panel presenting a calendar for date/time selection.<br><br>
 * Keyboard navigation:<br><br>
 * Previous/next year: CTRL + left/right arrow or down/up arrow.<br>
 * Previous/next month: SHIFT + left/right arrow or down/up arrow.<br>
 * Previous/next week: ALT + up/down arrow.<br>
 * Previous/next day: ALT + left/right arrow.<br>
 * Previous/next hour: SHIFT-ALT + left/right arrow or down/up arrow.<br>
 * Previous/next minute: CTRL-ALT + left/right arrow or down/up arrow.
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
		public static final ControlKey<CommandControl> PREVIOUS_YEAR = CommandControl.key(keyStroke(VK_DOWN, CTRL_DOWN_MASK));
		/**
		 * Select the next year.<br>
		 * Default key stroke: CTRL-UP ARROW
		 */
		public static final ControlKey<CommandControl> NEXT_YEAR = CommandControl.key(keyStroke(VK_UP, CTRL_DOWN_MASK));
		/**
		 * Select the previous month.<br>
		 * Default key stroke: SHIFT-DOWN ARROW
		 */
		public static final ControlKey<CommandControl> PREVIOUS_MONTH = CommandControl.key(keyStroke(VK_DOWN, SHIFT_DOWN_MASK));
		/**
		 * Select the next month.<br>
		 * Default key stroke: SHIFT-UP ARROW
		 */
		public static final ControlKey<CommandControl> NEXT_MONTH = CommandControl.key(keyStroke(VK_UP, SHIFT_DOWN_MASK));
		/**
		 * Select the previous week.<br>
		 * Default key stroke: ALT-UP ARROW
		 */
		public static final ControlKey<CommandControl> PREVIOUS_WEEK = CommandControl.key(keyStroke(VK_UP, ALT_DOWN_MASK));
		/**
		 * Select the next week.<br>
		 * Default key stroke: ALT-DOWN ARROW
		 */
		public static final ControlKey<CommandControl> NEXT_WEEK = CommandControl.key(keyStroke(VK_DOWN, ALT_DOWN_MASK));
		/**
		 * Select the previous day.<br>
		 * Default key stroke: ALT-LEFT ARROW
		 */
		public static final ControlKey<CommandControl> PREVIOUS_DAY = CommandControl.key(keyStroke(VK_LEFT, ALT_DOWN_MASK));
		/**
		 * Select the next day.<br>
		 * Default key stroke: ALT-RIGHT ARROW
		 */
		public static final ControlKey<CommandControl> NEXT_DAY = CommandControl.key(keyStroke(VK_RIGHT, ALT_DOWN_MASK));
		/**
		 * Select the previous hour.<br>
		 * Default key stroke: SHIFT-ALT-DOWN ARROW
		 */
		public static final ControlKey<CommandControl> PREVIOUS_HOUR = CommandControl.key(keyStroke(VK_DOWN, SHIFT_DOWN_MASK | ALT_DOWN_MASK));
		/**
		 * Select the next hour.<br>
		 * Default key stroke: SHIFT-ALT-UP ARROW
		 */
		public static final ControlKey<CommandControl> NEXT_HOUR = CommandControl.key(keyStroke(VK_UP, SHIFT_DOWN_MASK | ALT_DOWN_MASK));
		/**
		 * Select the previous minute.<br>
		 * Default key stroke: CTRL-ALT-DOWN ARROW
		 */
		public static final ControlKey<CommandControl> PREVIOUS_MINUTE = CommandControl.key(keyStroke(VK_DOWN, CTRL_DOWN_MASK | ALT_DOWN_MASK));
		/**
		 * Select the next minute.<br>
		 * Default key stroke: CTRL-ALT-UP ARROW
		 */
		public static final ControlKey<CommandControl> NEXT_MINUTE = CommandControl.key(keyStroke(VK_UP, CTRL_DOWN_MASK | ALT_DOWN_MASK));

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

	private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL);
	private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

	private final Value<LocalDate> localDateValue;
	private final Value<LocalDateTime> localDateTimeValue;

	private final Value<Integer> yearValue;
	private final Value<Month> monthValue;
	private final Value<Integer> dayValue;
	private final Value<Integer> hourValue;
	private final Value<Integer> minuteValue;
	private final State todaySelected;

	private final Map<Integer, JToggleButton> dayButtons;
	private final Map<Integer, State> dayStates;
	private final JPanel dayGridPanel;
	private final List<JLabel> dayFillLabels;
	private final JLabel formattedDateLabel;
	private final boolean includeTime;
	private final boolean includeTodayButton;

	CalendarPanel(DefaultBuilder builder) {
		this.includeTime = builder.includeTime;
		this.includeTodayButton = builder.includeTodayButton;
		LocalDateTime dateTime = builder.initialValue == null ? LocalDateTime.now() : builder.initialValue;
		yearValue = Value.nonNull(dateTime.getYear())
						.listener(this::updateDateTime)
						.listener(new LayoutDayPanelListener())
						.build();
		monthValue = Value.nonNull(dateTime.getMonth())
						.listener(this::updateDateTime)
						.listener(new LayoutDayPanelListener())
						.build();
		dayValue = Value.nonNull(dateTime.getDayOfMonth())
						.listener(this::updateDateTime)
						.build();
		if (includeTime) {
			hourValue = Value.nonNull(dateTime.getHour())
							.listener(this::updateDateTime)
							.build();
			minuteValue = Value.nonNull(dateTime.getMinute())
							.listener(this::updateDateTime)
							.build();
		}
		else {
			hourValue = Value.nonNull(0).build();
			minuteValue = Value.nonNull(0).build();
		}
		localDateValue = Value.nullable(createLocalDateTime().toLocalDate()).build();
		localDateTimeValue = Value.nullable(createLocalDateTime()).build();
		todaySelected = State.state(todaySelected());
		dayStates = createDayStates();
		dayButtons = createDayButtons();
		dayFillLabels = IntStream.rangeClosed(0, MAX_DAY_FILLERS + 1).mapToObj(counter -> new JLabel()).collect(Collectors.toList());
		dayGridPanel = new JPanel(new GridLayout(DAY_GRID_ROWS, DAYS_IN_WEEK));
		formattedDateLabel = new JLabel("", SwingConstants.CENTER);
		formattedDateLabel.setBorder(emptyBorder());
		initializeUI();
		addKeyEvents(builder.controlKeyStrokes);
		updateFormattedDate();
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
	 * Requests input focus for the current day button
	 */
	public void requestCurrentDayButtonFocus() {
		dayButtons.get(dayValue.get()).requestFocusInWindow();
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

		private final ControlKeyStrokes controlKeyStrokes = controlKeyStrokes(ControlKeys.class);

		private LocalDateTime initialValue;
		private boolean includeTime = false;
		private boolean includeTodayButton = false;

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
			controlKeyStrokes.keyStroke(controlKey).set(keyStroke);
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
		boolean dayPanelHasFocus = dayPanelHasFocus();
		if (unit.isDateBased()) {
			setYearMonthDay(getLocalDate().minus(1, unit));
		}
		else {
			setYearMonthDayHourMinute(getLocalDateTime().minus(1, unit));
		}
		if (dayPanelHasFocus) {
			requestCurrentDayButtonFocus();
		}
	}

	private void addOne(ChronoUnit unit) {
		boolean dayPanelHasFocus = dayPanelHasFocus();
		if (unit.isDateBased()) {
			setYearMonthDay(getLocalDate().plus(1, unit));
		}
		else {
			setYearMonthDayHourMinute(getLocalDateTime().plus(1, unit));
		}
		if (dayPanelHasFocus) {
			requestCurrentDayButtonFocus();
		}
	}

	private Map<Integer, State> createDayStates() {
		Map<Integer, State> states = IntStream.rangeClosed(1, MAX_DAYS_IN_MONTH).boxed()
						.collect(Collectors.toMap(Integer::valueOf, this::createDayState));
		State.group(states.values());

		return states;
	}

	private State createDayState(int dayOfMonth) {
		State dayState = State.state(dayValue.get().intValue() == dayOfMonth);
		dayState.addConsumer(selected -> {
			if (selected) {
				dayValue.set(dayOfMonth);
			}
		});

		return dayState;
	}

	private Map<Integer, JToggleButton> createDayButtons() {
		Insets margin = new Insets(0, 0, 0, 0);

		return dayStates.entrySet().stream()
						.collect(Collectors.toMap(Map.Entry::getKey, entry -> toggleButton(entry.getValue())
										.text(Integer.toString(entry.getKey()))
										.margin(margin)
										.build()));
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
		return button(commandControl(this::selectToday))
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

	private void layoutDayPanel() {
		boolean dayPanelHasFocus = dayPanelHasFocus();
		if (dayPanelHasFocus) {//otherwise, the focus jumps to the first field (month)
			dayGridPanel.requestFocusInWindow();
		}
		dayGridPanel.removeAll();
		int firstDayOfMonth = LocalDate.of(yearValue.get(), monthValue.get(), 1).getDayOfWeek().getValue();
		int fieldCount = 0;
		int fillerCount = 0;
		for (int i = 1; i < firstDayOfMonth; i++) {
			dayGridPanel.add(dayFillLabels.get(fillerCount++));
			fieldCount++;
		}
		YearMonth yearMonth = YearMonth.of(yearValue.get(), monthValue.get());
		for (int dayOfMonth = 1; dayOfMonth <= yearMonth.lengthOfMonth(); dayOfMonth++) {
			dayGridPanel.add(dayButtons.get(dayOfMonth));
			fieldCount++;
		}
		while (fieldCount++ < 42) {
			dayGridPanel.add(dayFillLabels.get(fillerCount++));
		}
		validate();
		repaint();
		if (dayPanelHasFocus) {
			requestCurrentDayButtonFocus();
		}
	}

	private boolean dayPanelHasFocus() {
		return dayGridPanel.isAncestorOf(FocusManager.getCurrentManager().getFocusOwner());
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
		dayStates.get(dayValue.get() > yearMonth.lengthOfMonth() ? yearMonth.lengthOfMonth() : dayValue.get()).set(true);
		LocalDateTime localDateTime = createLocalDateTime();
		localDateValue.set(localDateTime.toLocalDate());
		localDateTimeValue.set(localDateTime);
		todaySelected.set(todaySelected());
		if (SwingUtilities.isEventDispatchThread()) {
			updateFormattedDate();
		}
		else {
			SwingUtilities.invokeLater(this::updateFormattedDate);
		}
	}

	private boolean todaySelected() {
		return getLocalDate().equals(LocalDate.now());
	}

	private void selectToday() {
		dayGridPanel.requestFocusInWindow();//prevent focus flickering
		LocalDate now = LocalDate.now();
		setLocalDateTime(getLocalDateTime().withYear(now.getYear()).withMonth(now.getMonthValue()).withDayOfMonth(now.getDayOfMonth()));
		requestCurrentDayButtonFocus();
	}

	private void updateFormattedDate() {
		formattedDateLabel.setText(dateFormatter.format(getLocalDateTime()) + (includeTime ? ", " + timeFormatter.format(getLocalDateTime()) : ""));
	}

	private void addKeyEvents(ControlKeyStrokes controlKeyStrokes) {
		controlKeyStrokes.keyStroke(PREVIOUS_YEAR).optional().ifPresent(keyStroke ->
						addKeyEvent(keyStroke, commandControl(this::previousYear)));
		controlKeyStrokes.keyStroke(NEXT_YEAR).optional().ifPresent(keyStroke ->
						addKeyEvent(keyStroke, commandControl(this::nextYear)));
		controlKeyStrokes.keyStroke(PREVIOUS_MONTH).optional().ifPresent(keyStroke ->
						addKeyEvent(keyStroke, commandControl(this::previousMonth)));
		controlKeyStrokes.keyStroke(NEXT_MONTH).optional().ifPresent(keyStroke ->
						addKeyEvent(keyStroke, commandControl(this::nextMonth)));
		controlKeyStrokes.keyStroke(PREVIOUS_WEEK).optional().ifPresent(keyStroke ->
						addKeyEvent(keyStroke, commandControl(this::previousWeek)));
		controlKeyStrokes.keyStroke(NEXT_WEEK).optional().ifPresent(keyStroke ->
						addKeyEvent(keyStroke, commandControl(this::nextWeek)));
		controlKeyStrokes.keyStroke(PREVIOUS_DAY).optional().ifPresent(keyStroke ->
						addKeyEvent(keyStroke, commandControl(this::previousDay)));
		controlKeyStrokes.keyStroke(NEXT_DAY).optional().ifPresent(keyStroke ->
						addKeyEvent(keyStroke, commandControl(this::nextDay)));
		if (includeTime) {
			controlKeyStrokes.keyStroke(PREVIOUS_HOUR).optional().ifPresent(keyStroke ->
							addKeyEvent(keyStroke, commandControl(this::previousHour)));
			controlKeyStrokes.keyStroke(NEXT_HOUR).optional().ifPresent(keyStroke ->
							addKeyEvent(keyStroke, commandControl(this::nextHour)));
			controlKeyStrokes.keyStroke(PREVIOUS_MINUTE).optional().ifPresent(keyStroke ->
							addKeyEvent(keyStroke, commandControl(this::previousMinute)));
			controlKeyStrokes.keyStroke(NEXT_MINUTE).optional().ifPresent(keyStroke ->
							addKeyEvent(keyStroke, commandControl(this::nextMinute)));
		}
	}

	private void addKeyEvent(KeyStroke keyStroke, Control control) {
		KeyEvents.builder(keyStroke)
						.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
						.action(control)
						.enable(this);
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

	private static JPanel createDayHeaderPanel() {
		return panel(new GridLayout(1, DAYS_IN_WEEK))
						.add(createDayLabel(DayOfWeek.MONDAY))
						.add(createDayLabel(DayOfWeek.TUESDAY))
						.add(createDayLabel(DayOfWeek.WEDNESDAY))
						.add(createDayLabel(DayOfWeek.THURSDAY))
						.add(createDayLabel(DayOfWeek.FRIDAY))
						.add(createDayLabel(DayOfWeek.SATURDAY))
						.add(createDayLabel(DayOfWeek.SUNDAY))
						.build();
	}

	private static JLabel createDayLabel(DayOfWeek dayOfWeek) {
		return label(dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()))
						.horizontalAlignment(SwingConstants.CENTER)
						.border(emptyBorder())
						.build();
	}

	private static List<Item<Month>> createMonthItems() {
		return Arrays.stream(Month.values())
						.map(month -> Item.item(month, month.getDisplayName(TextStyle.SHORT, Locale.getDefault())))
						.collect(Collectors.toList());
	}

	private final class LayoutDayPanelListener implements Runnable {

		@Override
		public void run() {
			if (SwingUtilities.isEventDispatchThread()) {
				layoutDayPanel();
			}
			else {
				SwingUtilities.invokeLater(CalendarPanel.this::layoutDayPanel);
			}
		}
	}
}
