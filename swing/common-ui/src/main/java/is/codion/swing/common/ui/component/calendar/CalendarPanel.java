/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.calendar;

import is.codion.common.event.EventDataListener;
import is.codion.common.item.Item;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.component.Components;

import javax.swing.BorderFactory;
import javax.swing.FocusManager;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static is.codion.swing.common.ui.control.Control.control;
import static is.codion.swing.common.ui.layout.Layouts.*;
import static java.util.Objects.requireNonNull;

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
  private final State todaySelectedState;

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
    todaySelectedState = State.state(isTodaySelected());
    dayStates = createDayStates();
    dayButtons = createDayButtons();
    dayFillLabels = IntStream.rangeClosed(0, MAX_DAY_FILLERS + 1).mapToObj(counter -> new JLabel()).collect(Collectors.toList());
    dayGridPanel = new JPanel(gridLayout(6, DAYS_IN_WEEK));
    formattedDateLabel = new JLabel("", SwingConstants.CENTER);
    formattedDateLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    initializeUI();
    updateFormattedDate();
    bindEvents();
  }

  /**
   * Sets the date to present in this calendar
   * @param date the date to set
   */
  public void setDate(LocalDate date) {
    setDateTime(requireNonNull(date).atStartOfDay());
  }

  /**
   * @return the date currently displayed in this calendar
   */
  public LocalDate getDate() {
    return LocalDate.of(yearValue.get(), monthValue.get(), dayValue.get());
  }

  /**
   * Sets the date/time to present in this calendar.
   * @param dateTime the date/time to set
   */
  public void setDateTime(LocalDateTime dateTime) {
    requireNonNull(dateTime);
    yearValue.set(dateTime.getYear());
    monthValue.set(dateTime.getMonth());
    dayValue.set(dateTime.getDayOfMonth());
    if (includeTime) {
      hourValue.set(dateTime.getHour());
      minuteValue.set(dateTime.getMinute());
    }
  }

  /**
   * @return the date/time currently displayed in this calendar
   */
  public LocalDateTime getDateTime() {
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
  public void addDateListener(EventDataListener<LocalDate> listener) {
    localDateValue.addDataListener(listener);
  }

  /**
   * @param listener a listener notified each time the date or time changes
   */
  public void addDateTimeListener(EventDataListener<LocalDateTime> listener) {
    localDateTimeValue.addDataListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public void removeDateListener(EventDataListener<LocalDate> listener) {
    localDateValue.removeDataListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public void removeDateTimeListener(EventDataListener<LocalDateTime> listener) {
    localDateTimeValue.removeDataListener(listener);
  }

  /**
   * @return  a new {@link CalendarPanel} without time fields.
   */
  public static CalendarPanel dateCalendarPanel() {
    return new CalendarPanel(false);
  }

  /**
   * @return  a new {@link CalendarPanel} with time fields.
   */
  public static CalendarPanel dateTimeCalendarPanel() {
    return new CalendarPanel(true);
  }

  void previousMonth() {
    LocalDate previousMonth = getDate().minus(1, ChronoUnit.MONTHS);
    monthValue.set(previousMonth.getMonth());
    yearValue.set(previousMonth.getYear());
  }

  void nextMonth() {
    LocalDate nextMonth = getDate().plus(1, ChronoUnit.MONTHS);
    monthValue.set(nextMonth.getMonth());
    yearValue.set(nextMonth.getYear());
  }

  void previousYear() {
    yearValue.set(yearValue.get() - 1);
  }

  void nextYear() {
    yearValue.set(yearValue.get() + 1);
  }

  void previousWeek() {
    boolean dayPanelHasFocus = dayPanelHasFocus();
    setYearMonthDay(getDate().minus(1, ChronoUnit.WEEKS));
    if (dayPanelHasFocus) {
      requestCurrentDayButtonFocus();
    }
  }

  void nextWeek() {
    boolean dayPanelHasFocus = dayPanelHasFocus();
    setYearMonthDay(getDate().plus(1, ChronoUnit.WEEKS));
    if (dayPanelHasFocus) {
      requestCurrentDayButtonFocus();
    }
  }

  void previousDay() {
    boolean dayPanelHasFocus = dayPanelHasFocus();
    setYearMonthDay(getDate().minus(1, ChronoUnit.DAYS));
    if (dayPanelHasFocus) {
      requestCurrentDayButtonFocus();
    }
  }

  void nextDay() {
    boolean dayPanelHasFocus = dayPanelHasFocus();
    setYearMonthDay(getDate().plus(1, ChronoUnit.DAYS));
    if (dayPanelHasFocus) {
      requestCurrentDayButtonFocus();
    }
  }

  void previousHour() {
    boolean dayPanelHasFocus = dayPanelHasFocus();
    setYearMonthDayHourMinute(getDateTime().minus(1, ChronoUnit.HOURS));
    if (dayPanelHasFocus) {
      requestCurrentDayButtonFocus();
    }
  }

  void nextHour() {
    boolean dayPanelHasFocus = dayPanelHasFocus();
    setYearMonthDayHourMinute(getDateTime().plus(1, ChronoUnit.HOURS));
    if (dayPanelHasFocus) {
      requestCurrentDayButtonFocus();
    }
  }

  void previousMinute() {
    boolean dayPanelHasFocus = dayPanelHasFocus();
    setYearMonthDayHourMinute(getDateTime().minus(1, ChronoUnit.MINUTES));
    if (dayPanelHasFocus) {
      requestCurrentDayButtonFocus();
    }
  }

  void nextMinute() {
    boolean dayPanelHasFocus = dayPanelHasFocus();
    setYearMonthDayHourMinute(getDateTime().plus(1, ChronoUnit.MINUTES));
    if (dayPanelHasFocus) {
      requestCurrentDayButtonFocus();
    }
  }

  private Map<Integer, State> createDayStates() {
    Map<Integer, State> states = new HashMap<>();
    State.Group stateGroup = State.group();
    for (int dayOfMonth = 1; dayOfMonth <= MAX_DAYS_IN_MONTH; dayOfMonth++) {
      State state = createDayState(dayOfMonth);
      stateGroup.addState(state);
      states.put(dayOfMonth, state);
    }

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
    Map<Integer, JToggleButton> buttons = new HashMap<>();
    dayStates.forEach((dayOfMonth, dayState) -> buttons.put(dayOfMonth, Components.toggleButton(dayState)
            .caption(Integer.toString(dayOfMonth))
            .build()));

    return buttons;
  }

  private void initializeUI() {
    setLayout(borderLayout());
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    add(createNorthPanel(), BorderLayout.NORTH);
    add(createDayPanel(), BorderLayout.CENTER);
    addKeyEvents();
    layoutDayPanel();
  }

  private JPanel createNorthPanel() {
    JPanel northCenterPanel = new JPanel(flowLayout(FlowLayout.CENTER));
    northCenterPanel.add(createYearMonthHourMinutePanel());
    northCenterPanel.setBorder(BorderFactory.createTitledBorder(""));

    JPanel northNorthPanel = new JPanel(borderLayout());
    northNorthPanel.add(formattedDateLabel, BorderLayout.CENTER);
    northNorthPanel.setBorder(BorderFactory.createTitledBorder(""));

    JPanel northPanel = new JPanel(borderLayout());
    northPanel.add(northNorthPanel, BorderLayout.NORTH);
    northPanel.add(northCenterPanel, BorderLayout.CENTER);

    return northPanel;
  }

  private JPanel createYearMonthHourMinutePanel() {
    JSpinner yearSpinner = createYearSpinner();
    JSpinner monthSpinner = createMonthSpinner(yearSpinner);
    JSpinner hourSpinner = createHourSpinner();
    JSpinner minuteSpinner = createMinuteSpinner();

    JPanel yearMonthHourMinutePanel = new JPanel(flexibleGridLayout(1, 0));
    yearMonthHourMinutePanel.add(monthSpinner);
    yearMonthHourMinutePanel.add(yearSpinner);
    if (includeTime) {
      yearMonthHourMinutePanel.add(new JLabel(" "));
      yearMonthHourMinutePanel.add(hourSpinner);
      yearMonthHourMinutePanel.add(new JLabel(":", SwingConstants.CENTER));
      yearMonthHourMinutePanel.add(minuteSpinner);
    }
    yearMonthHourMinutePanel.add(createSelectTodayButton());

    return yearMonthHourMinutePanel;
  }

  private JButton createSelectTodayButton() {
    return Components.button(control(this::selectToday))
            .caption(MESSAGES.getString("today"))
            .mnemonic(MESSAGES.getString("today_mnemonic").charAt(0))
            .enabledState(todaySelectedState.getReversedObserver())
            .build();
  }

  private JPanel createDayPanel() {
    JPanel dayPanel = new JPanel(borderLayout());
    dayPanel.add(createDayHeaderPanel(), BorderLayout.NORTH);
    dayPanel.add(dayGridPanel, BorderLayout.CENTER);
    dayPanel.setBorder(BorderFactory.createTitledBorder(""));

    return dayPanel;
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
    todaySelectedState.set(isTodaySelected());
    SwingUtilities.invokeLater(this::updateFormattedDate);
  }

  private boolean isTodaySelected() {
    return getDate().equals(LocalDate.now());
  }

  private void selectToday() {
    LocalDate now = LocalDate.now();
    setDateTime(getDateTime().withYear(now.getYear()).withMonth(now.getMonthValue()).withDayOfMonth(now.getDayOfMonth()));
    requestCurrentDayButtonFocus();
  }

  private void updateFormattedDate() {
    formattedDateLabel.setText(dateFormatter.format(getDateTime()) + (includeTime ? " " + timeFormatter.format(getDateTime()) : ""));
  }

  private void addKeyEvents() {
    KeyEvents.Builder keyEvent = KeyEvents.builder()
            .condition(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .onKeyPressed();
    keyEvent.modifiers(InputEvent.CTRL_DOWN_MASK)
            .keyEvent(KeyEvent.VK_LEFT)
            .action(control(this::previousYear))
            .enable(this);
    keyEvent.keyEvent(KeyEvent.VK_DOWN)
            .enable(this);
    keyEvent.keyEvent(KeyEvent.VK_RIGHT)
            .action(control(this::nextYear))
            .enable(this);
    keyEvent.keyEvent(KeyEvent.VK_UP)
            .enable(this);
    keyEvent.modifiers(InputEvent.SHIFT_DOWN_MASK)
            .keyEvent(KeyEvent.VK_LEFT)
            .action(control(this::previousMonth))
            .enable(this);
    keyEvent.keyEvent(KeyEvent.VK_DOWN)
            .enable(this);
    keyEvent.keyEvent(KeyEvent.VK_RIGHT)
            .action(control(this::nextMonth))
            .enable(this);
    keyEvent.keyEvent(KeyEvent.VK_UP)
            .enable(this);
    keyEvent.modifiers(InputEvent.ALT_DOWN_MASK)
            .keyEvent(KeyEvent.VK_UP)
            .action(control(this::previousWeek))
            .enable(this);
    keyEvent.keyEvent(KeyEvent.VK_DOWN)
            .action(control(this::nextWeek))
            .enable(this);
    keyEvent.keyEvent(KeyEvent.VK_LEFT)
            .action(control(this::previousDay))
            .enable(this);
    keyEvent.keyEvent(KeyEvent.VK_RIGHT)
            .action(control(this::nextDay))
            .enable(this);
    if (includeTime) {
      keyEvent.modifiers(InputEvent.SHIFT_DOWN_MASK + InputEvent.ALT_DOWN_MASK)
              .keyEvent(KeyEvent.VK_LEFT)
              .action(control(this::previousHour))
              .enable(this);
      keyEvent.keyEvent(KeyEvent.VK_DOWN)
              .enable(this);
      keyEvent.keyEvent(KeyEvent.VK_RIGHT)
              .action(control(this::nextHour))
              .enable(this);
      keyEvent.keyEvent(KeyEvent.VK_UP)
              .enable(this);
      keyEvent.modifiers(InputEvent.CTRL_DOWN_MASK + InputEvent.ALT_DOWN_MASK)
              .keyEvent(KeyEvent.VK_LEFT)
              .action(control(this::previousMinute))
              .enable(this);
      keyEvent.keyEvent(KeyEvent.VK_DOWN)
              .enable(this);
      keyEvent.keyEvent(KeyEvent.VK_RIGHT)
              .action(control(this::nextMinute))
              .enable(this);
      keyEvent.keyEvent(KeyEvent.VK_UP)
              .enable(this);
    }
  }

  private void bindEvents() {
    yearValue.addListener(this::updateDateTime);
    monthValue.addListener(this::updateDateTime);
    dayValue.addListener(this::updateDateTime);
    hourValue.addListener(this::updateDateTime);
    minuteValue.addListener(this::updateDateTime);
    yearValue.addListener(() -> SwingUtilities.invokeLater(this::layoutDayPanel));
    monthValue.addListener(() -> SwingUtilities.invokeLater(this::layoutDayPanel));
  }

  private JSpinner createYearSpinner() {
    return Components.integerSpinner(new SpinnerNumberModel(0, -9999, 9999, 1), yearValue)
            .horizontalAlignment(SwingConstants.CENTER)
            .columns(YEAR_COLUMNS)
            .editable(false)
            .groupingUsed(false)
            .onBuild(CalendarPanel::removeCtrlLeftRightArrowKeyEvents)
            .build();
  }

  private JSpinner createMonthSpinner(JSpinner yearSpinner) {
    List<Item<Month>> monthItems = createMonthItems();
    JSpinner monthSpinner = Components.itemSpinner(new SpinnerListModel(monthItems), monthValue)
            .horizontalAlignment(SwingConstants.CENTER)
            .editable(false)
            .onBuild(CalendarPanel::removeCtrlLeftRightArrowKeyEvents)
            .build();
    JFormattedTextField monthTextField = ((JSpinner.DefaultEditor) monthSpinner.getEditor()).getTextField();
    monthTextField.setFont(((JSpinner.DefaultEditor) yearSpinner.getEditor()).getTextField().getFont());
    monthItems.stream()
            .mapToInt(item -> item.getCaption().length())
            .max()
            .ifPresent(monthTextField::setColumns);

    return monthSpinner;
  }

  private JSpinner createHourSpinner() {
    return Components.integerSpinner(new SpinnerNumberModel(0, 0, 23, 1), hourValue)
            .horizontalAlignment(SwingConstants.CENTER)
            .columns(TIME_COLUMNS)
            .editable(false)
            .decimalFormatPattern("00")
            .onBuild(CalendarPanel::removeCtrlLeftRightArrowKeyEvents)
            .build();
  }

  private JSpinner createMinuteSpinner() {
    return Components.integerSpinner(new SpinnerNumberModel(0, 0, 59, 1), minuteValue)
            .horizontalAlignment(SwingConstants.CENTER)
            .columns(TIME_COLUMNS)
            .editable(false)
            .decimalFormatPattern("00")
            .onBuild(CalendarPanel::removeCtrlLeftRightArrowKeyEvents)
            .build();
  }

  private static JSpinner removeCtrlLeftRightArrowKeyEvents(JSpinner spinner) {
    InputMap inputMap = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().getInputMap(JComponent.WHEN_FOCUSED);
    //so it doesn't interfere with keyboard navigation when it has focus
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.CTRL_DOWN_MASK,false), "none");
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.CTRL_DOWN_MASK,false), "none");
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.SHIFT_DOWN_MASK,false), "none");
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.SHIFT_DOWN_MASK,false), "none");

    return spinner;
  }

  private static JPanel createDayHeaderPanel() {
    JPanel headerPanel = new JPanel(gridLayout(1, DAYS_IN_WEEK));
    headerPanel.add(createDayLabel(DayOfWeek.MONDAY));
    headerPanel.add(createDayLabel(DayOfWeek.TUESDAY));
    headerPanel.add(createDayLabel(DayOfWeek.WEDNESDAY));
    headerPanel.add(createDayLabel(DayOfWeek.THURSDAY));
    headerPanel.add(createDayLabel(DayOfWeek.FRIDAY));
    headerPanel.add(createDayLabel(DayOfWeek.SATURDAY));
    headerPanel.add(createDayLabel(DayOfWeek.SUNDAY));

    return headerPanel;
  }

  private static JLabel createDayLabel(DayOfWeek dayOfWeek) {
    JLabel label = new JLabel(dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()), SwingConstants.CENTER);
    label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    return label;
  }

  private static List<Item<Month>> createMonthItems() {
    return Arrays.stream(Month.values())
            .map(month -> Item.item(month, month.getDisplayName(TextStyle.FULL, Locale.getDefault())))
            .collect(Collectors.toList());
  }
}
