/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.calendar;

import is.codion.common.item.Item;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.KeyboardShortcut;
import is.codion.swing.common.ui.component.panel.PanelBuilder;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static is.codion.swing.common.ui.KeyboardShortcut.keyStrokeValue;
import static is.codion.swing.common.ui.border.Borders.emptyBorder;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.component.calendar.CalendarPanel.KeyboardShortcuts.*;
import static is.codion.swing.common.ui.control.Control.control;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;
import static java.awt.event.InputEvent.*;
import static java.awt.event.KeyEvent.*;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static javax.swing.BorderFactory.createTitledBorder;

/**
 * A panel presenting a calendar for date/time selection.<br><br>
 * For a {@link CalendarPanel} without time fields use the {@link #dateCalendarPanel()} factory method.<br>
 * For a {@link CalendarPanel} with time fields use the {@link #dateTimeCalendarPanel()} factory method.<br><br>
 * Keyboard navigation:<br><br>
 * Previous/next year: CTRL + left/right arrow or down/up arrow.<br>
 * Previous/next month: SHIFT + left/right arrow or down/up arrow.<br>
 * Previous/next week: ALT + up/down arrow.<br>
 * Previous/next day: ALT + left/right arrow.<br>
 * Previous/next hour: SHIFT-ALT + left/right arrow or down/up arrow.<br>
 * Previous/next minute: CTRL-ALT + left/right arrow or down/up arrow.
 */
public final class CalendarPanel extends JPanel {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(CalendarPanel.class.getName());

  /**
   * The available keyboard shortcuts.
   */
  public enum KeyboardShortcuts implements KeyboardShortcut {
    PREVIOUS_YEAR,
    NEXT_YEAR,
    PREVIOUS_MONTH,
    NEXT_MONTH,
    PREVIOUS_WEEK,
    NEXT_WEEK,
    PREVIOUS_DAY,
    NEXT_DAY,
    PREVIOUS_HOUR,
    NEXT_HOUR,
    PREVIOUS_MINUTE,
    NEXT_MINUTE;

    private static final Map<KeyboardShortcut, Value<KeyStroke>> KEYSTROKES = createDefaultKeystrokes();

    @Override
    public Value<KeyStroke> keyStroke() {
      return KEYSTROKES.get(this);
    }
  }

  private static final Set<Class<? extends Temporal>> SUPPORTED_TYPES =
          unmodifiableSet(new HashSet<>(asList(LocalDate.class, LocalDateTime.class)));

  private static final int YEAR_COLUMNS = 4;
  private static final int TIME_COLUMNS = 2;
  private static final int DAYS_IN_WEEK = 7;
  private static final int MAX_DAYS_IN_MONTH = 31;
  private static final int MAX_DAY_FILLERS = 14;

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

  CalendarPanel(boolean includeTime) {
    this.includeTime = includeTime;
    LocalDateTime dateTime = LocalDateTime.now();
    yearValue = Value.value(dateTime.getYear(), dateTime.getYear());
    monthValue = Value.value(dateTime.getMonth(), dateTime.getMonth());
    dayValue = Value.value(dateTime.getDayOfMonth(), dateTime.getDayOfMonth());
    if (includeTime) {
      hourValue = Value.value(dateTime.getHour(), dateTime.getHour());
      minuteValue = Value.value(dateTime.getMinute(), dateTime.getMinute());
    }
    else {
      hourValue = Value.value(0, 0);
      minuteValue = Value.value(0, 0);
    }
    localDateValue = Value.value(createLocalDateTime().toLocalDate());
    localDateTimeValue = Value.value(createLocalDateTime());
    todaySelected = State.state(todaySelected());
    dayStates = createDayStates();
    dayButtons = createDayButtons();
    dayFillLabels = IntStream.rangeClosed(0, MAX_DAY_FILLERS + 1).mapToObj(counter -> new JLabel()).collect(Collectors.toList());
    dayGridPanel = new JPanel(gridLayout(6, DAYS_IN_WEEK));
    formattedDateLabel = new JLabel("", SwingConstants.CENTER);
    formattedDateLabel.setBorder(emptyBorder());
    initializeUI();
    updateFormattedDate();
    bindEvents();
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
   * @param listener a listener notified each time the date changes
   */
  public void addLocalDateListener(Consumer<LocalDate> listener) {
    localDateValue.addDataListener(listener);
  }

  /**
   * @param listener a listener notified each time the date or time changes
   */
  public void addLocalDateTimeListener(Consumer<LocalDateTime> listener) {
    localDateTimeValue.addDataListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public void removeLocalDateListener(Consumer<LocalDate> listener) {
    localDateValue.removeDataListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public void removeLocalDateTimeListener(Consumer<LocalDateTime> listener) {
    localDateTimeValue.removeDataListener(listener);
  }

  /**
   * @return a new {@link CalendarPanel} without time fields.
   */
  public static CalendarPanel dateCalendarPanel() {
    return new CalendarPanel(false);
  }

  /**
   * @return a new {@link CalendarPanel} with time fields.
   */
  public static CalendarPanel dateTimeCalendarPanel() {
    return new CalendarPanel(true);
  }

  /**
   * @return the temporal types supported by this calendar panel
   */
  public static Collection<Class<? extends Temporal>> supportedTypes() {
    return SUPPORTED_TYPES;
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
    dayState.addDataListener(selected -> {
      if (selected) {
        dayValue.set(dayOfMonth);
      }
    });

    return dayState;
  }

  private Map<Integer, JToggleButton> createDayButtons() {
    return dayStates.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> toggleButton(entry.getValue())
                    .text(Integer.toString(entry.getKey()))
                    .build()));
  }

  private void initializeUI() {
    setLayout(borderLayout());
    setBorder(emptyBorder());
    add(createNorthPanel(), BorderLayout.NORTH);
    add(createDayPanel(), BorderLayout.CENTER);
    addKeyEvents();
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
      yearMonthHourMinutePanel.addAll(new JLabel(" "), hourSpinner, new JLabel(":", SwingConstants.CENTER), minuteSpinner);
    }

    return yearMonthHourMinutePanel.add(createSelectTodayButton()).build();
  }

  private JButton createSelectTodayButton() {
    return button(control(this::selectToday))
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
    formattedDateLabel.setText(dateFormatter.format(getLocalDateTime()) + (includeTime ? " " + timeFormatter.format(getLocalDateTime()) : ""));
  }

  private void addKeyEvents() {
    KeyEvents.Builder keyEvent = KeyEvents.builder()
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    keyEvent.keyStroke(PREVIOUS_YEAR.keyStroke().get())
            .action(control(this::previousYear))
            .enable(this);
    keyEvent.keyStroke(NEXT_YEAR.keyStroke().get())
            .action(control(this::nextYear))
            .enable(this);
    keyEvent.keyStroke(PREVIOUS_MONTH.keyStroke().get())
            .action(control(this::previousMonth))
            .enable(this);
    keyEvent.keyStroke(NEXT_MONTH.keyStroke().get())
            .action(control(this::nextMonth))
            .enable(this);
    keyEvent.keyStroke(PREVIOUS_WEEK.keyStroke().get())
            .action(control(this::previousWeek))
            .enable(this);
    keyEvent.keyStroke(NEXT_WEEK.keyStroke().get())
            .action(control(this::nextWeek))
            .enable(this);
    keyEvent.keyStroke(PREVIOUS_DAY.keyStroke().get())
            .action(control(this::previousDay))
            .enable(this);
    keyEvent.keyStroke(NEXT_DAY.keyStroke().get())
            .action(control(this::nextDay))
            .enable(this);
    if (includeTime) {
      keyEvent.keyStroke(PREVIOUS_HOUR.keyStroke().get())
              .action(control(this::previousHour))
              .enable(this);
      keyEvent.keyStroke(NEXT_HOUR.keyStroke().get())
              .action(control(this::nextHour))
              .enable(this);
      keyEvent.keyStroke(PREVIOUS_MINUTE.keyStroke().get())
              .action(control(this::previousMinute))
              .enable(this);
      keyEvent.keyStroke(NEXT_MINUTE.keyStroke().get())
              .action(control(this::nextMinute))
              .enable(this);
    }
  }

  private void bindEvents() {
    yearValue.addListener(this::updateDateTime);
    monthValue.addListener(this::updateDateTime);
    dayValue.addListener(this::updateDateTime);
    hourValue.addListener(this::updateDateTime);
    minuteValue.addListener(this::updateDateTime);
    Runnable layoutDayPanelListener = new LayoutDayPanelListener();
    yearValue.addListener(layoutDayPanelListener);
    monthValue.addListener(layoutDayPanelListener);
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
    inputMap.put(KeyStroke.getKeyStroke(VK_LEFT, CTRL_DOWN_MASK, false), "none");
    inputMap.put(KeyStroke.getKeyStroke(VK_RIGHT, CTRL_DOWN_MASK, false), "none");
    inputMap.put(KeyStroke.getKeyStroke(VK_LEFT, SHIFT_DOWN_MASK, false), "none");
    inputMap.put(KeyStroke.getKeyStroke(VK_RIGHT, SHIFT_DOWN_MASK, false), "none");

    return spinner;
  }

  private static JPanel createDayHeaderPanel() {
    return gridLayoutPanel(1, DAYS_IN_WEEK)
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
            .map(month -> Item.item(month, month.getDisplayName(TextStyle.FULL, Locale.getDefault())))
            .collect(Collectors.toList());
  }

  private static Map<KeyboardShortcut, Value<KeyStroke>> createDefaultKeystrokes() {
    Map<KeyboardShortcut, Value<KeyStroke>> keyStrokes = new HashMap<>();
    keyStrokes.put(PREVIOUS_YEAR, keyStrokeValue(VK_DOWN, CTRL_DOWN_MASK));
    keyStrokes.put(NEXT_YEAR, keyStrokeValue(VK_UP, CTRL_DOWN_MASK));
    keyStrokes.put(PREVIOUS_MONTH, keyStrokeValue(VK_DOWN, SHIFT_DOWN_MASK));
    keyStrokes.put(NEXT_MONTH, keyStrokeValue(VK_UP, SHIFT_DOWN_MASK));
    keyStrokes.put(PREVIOUS_WEEK, keyStrokeValue(VK_UP, ALT_DOWN_MASK));
    keyStrokes.put(NEXT_WEEK, keyStrokeValue(VK_DOWN, ALT_DOWN_MASK));
    keyStrokes.put(PREVIOUS_DAY, keyStrokeValue(VK_LEFT, ALT_DOWN_MASK));
    keyStrokes.put(NEXT_DAY, keyStrokeValue(VK_RIGHT, ALT_DOWN_MASK));
    keyStrokes.put(PREVIOUS_HOUR, keyStrokeValue(VK_DOWN, SHIFT_DOWN_MASK | ALT_DOWN_MASK));
    keyStrokes.put(NEXT_HOUR, keyStrokeValue(VK_UP, SHIFT_DOWN_MASK | ALT_DOWN_MASK));
    keyStrokes.put(PREVIOUS_MINUTE, keyStrokeValue(VK_DOWN, CTRL_DOWN_MASK | ALT_DOWN_MASK));
    keyStrokes.put(NEXT_MINUTE, keyStrokeValue(VK_UP, CTRL_DOWN_MASK | ALT_DOWN_MASK));

    return keyStrokes;
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
