/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.calendar;

import is.codion.common.event.EventDataListener;
import is.codion.common.item.Item;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.swing.common.model.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.value.ComponentValues;

import javax.swing.BorderFactory;
import javax.swing.FocusManager;
import javax.swing.InputMap;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static is.codion.swing.common.ui.layout.Layouts.*;
import static java.util.Objects.requireNonNull;

/**
 * A panel presenting a calendar for date/time selection.<br><br>
 * For a {@link CalendarPanel} without time fields use the {@link #dateCalendarPanel()} factory method.<br>
 * For a {@link CalendarPanel} with time fields use the {@link #dateTimeCalendarPanel()} factory method.<br><br>
 * Keyboard navigation:<br><br>
 * Previous/next year: SHIFT + left/right arrow.<br>
 * Previous/next month: CTRL + left/right arrow.<br>
 * Previous/next week: ALT + up/down arrow.<br>
 * Previous/next day: ALT + left/right arrow.<br>
 * Previous/next hour: CTRL-ALT + left/right arrow.<br>
 * Previous/next minute: SHIFT-ALT + left/right arrow.
 */
public final class CalendarPanel extends JPanel {

  private static final int YEAR_COLUMNS = 4;
  private static final int TIME_COLUMNS = 2;
  private static final int DAYS_IN_WEEK = 7;
  private static final int MONTHS_IN_YEAR = 12;
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

  private final Map<Integer, JToggleButton> dayButtons;
  private final Map<Integer, State> dayStates;
  private final JPanel dayGridPanel;
  private final List<JLabel> dayFillLabels;
  private final JLabel formattedDateLabel;
  private final boolean includeTime;

  CalendarPanel(final boolean includeTime) {
    this.includeTime = includeTime;
    final LocalDateTime dateTime = LocalDateTime.now();
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
    dayStates = createDayStates();
    dayButtons = createDayButtons();
    dayFillLabels = IntStream.rangeClosed(0, MAX_DAY_FILLERS + 1).mapToObj(counter -> new JLabel()).collect(Collectors.toList());
    dayGridPanel = new JPanel(gridLayout(6, DAYS_IN_WEEK));
    formattedDateLabel = new JLabel("", SwingConstants.CENTER);
    formattedDateLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    initializeUI();
    updateFormattedDate();
    bindEvents();
    Components.addInitialFocusHack(this, Control.control(() -> dayButtons.get(dayValue.get()).requestFocusInWindow()));
  }

  /**
   * Sets the date to present in this calendar
   * @param date the date to set
   */
  public void setDate(final LocalDate date) {
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
  public void setDateTime(final LocalDateTime dateTime) {
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
   * @param listener a listener notified each time the date changes
   */
  public void addDateListener(final EventDataListener<LocalDate> listener) {
    localDateValue.addDataListener(listener);
  }

  /**
   * @param listener a listener notified each time the date or time changes
   */
  public void addDateTimeListener(final EventDataListener<LocalDateTime> listener) {
    localDateTimeValue.addDataListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public void removeDateListener(final EventDataListener<LocalDate> listener) {
    localDateValue.removeDataListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public void removeDateTimeListener(final EventDataListener<LocalDateTime> listener) {
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
    final LocalDate previousMonth = getDate().minus(1, ChronoUnit.MONTHS);
    monthValue.set(previousMonth.getMonth());
    yearValue.set(previousMonth.getYear());
  }

  void nextMonth() {
    final LocalDate nextMonth = getDate().plus(1, ChronoUnit.MONTHS);
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
    final boolean dayPanelHasFocus = dayPanelHasFocus();
    setYearMonthDay(getDate().minus(1, ChronoUnit.WEEKS));
    if (dayPanelHasFocus) {
      requestCurrentDayButtonFocus();
    }
  }

  void nextWeek() {
    final boolean dayPanelHasFocus = dayPanelHasFocus();
    setYearMonthDay(getDate().plus(1, ChronoUnit.WEEKS));
    if (dayPanelHasFocus) {
      requestCurrentDayButtonFocus();
    }
  }

  void previousDay() {
    final boolean dayPanelHasFocus = dayPanelHasFocus();
    setYearMonthDay(getDate().minus(1, ChronoUnit.DAYS));
    if (dayPanelHasFocus) {
      requestCurrentDayButtonFocus();
    }
  }

  void nextDay() {
    final boolean dayPanelHasFocus = dayPanelHasFocus();
    setYearMonthDay(getDate().plus(1, ChronoUnit.DAYS));
    if (dayPanelHasFocus) {
      requestCurrentDayButtonFocus();
    }
  }

  void previousHour() {
    final boolean dayPanelHasFocus = dayPanelHasFocus();
    setYearMonthDayHourMinute(getDateTime().minus(1, ChronoUnit.HOURS));
    if (dayPanelHasFocus) {
      requestCurrentDayButtonFocus();
    }
  }

  void nextHour() {
    final boolean dayPanelHasFocus = dayPanelHasFocus();
    setYearMonthDayHourMinute(getDateTime().plus(1, ChronoUnit.HOURS));
    if (dayPanelHasFocus) {
      requestCurrentDayButtonFocus();
    }
  }

  void previousMinute() {
    final boolean dayPanelHasFocus = dayPanelHasFocus();
    setYearMonthDayHourMinute(getDateTime().minus(1, ChronoUnit.MINUTES));
    if (dayPanelHasFocus) {
      requestCurrentDayButtonFocus();
    }
  }

  void nextMinute() {
    final boolean dayPanelHasFocus = dayPanelHasFocus();
    setYearMonthDayHourMinute(getDateTime().plus(1, ChronoUnit.MINUTES));
    if (dayPanelHasFocus) {
      requestCurrentDayButtonFocus();
    }
  }

  private Map<Integer, State> createDayStates() {
    final Map<Integer, State> states = new HashMap<>();
    final State.Group stateGroup = State.group();
    for (int dayOfMonth = 1; dayOfMonth <= MAX_DAYS_IN_MONTH; dayOfMonth++) {
      final State state = createDayState(dayOfMonth);
      stateGroup.addState(state);
      states.put(dayOfMonth, state);
    }

    return states;
  }

  private State createDayState(final int dayOfMonth) {
    final State dayState = State.state(dayValue.get().intValue() == dayOfMonth);
    dayState.addDataListener(selected -> {
      if (selected) {
        dayValue.set(dayOfMonth);
      }
    });

    return dayState;
  }

  private Map<Integer, JToggleButton> createDayButtons() {
    final Map<Integer, JToggleButton> buttons = new HashMap<>();
    dayStates.forEach((dayOfMonth, dayState) -> buttons.put(dayOfMonth, ToggleControl.builder(dayState)
            .caption(Integer.toString(dayOfMonth))
            .build().createToggleButton()));

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
    final JPanel northCenterPanel = new JPanel(flowLayout(FlowLayout.CENTER));
    northCenterPanel.add(createYearMonthHourMinutePanel());
    northCenterPanel.setBorder(BorderFactory.createTitledBorder(""));

    final JPanel northNorthPanel = new JPanel(borderLayout());
    northNorthPanel.add(formattedDateLabel, BorderLayout.CENTER);
    northNorthPanel.setBorder(BorderFactory.createTitledBorder(""));

    final JPanel northPanel = new JPanel(borderLayout());
    northPanel.add(northNorthPanel, BorderLayout.NORTH);
    northPanel.add(northCenterPanel, BorderLayout.CENTER);

    return northPanel;
  }

  private JPanel createYearMonthHourMinutePanel() {
    final JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(hourValue.get().intValue(), -9999, 9999, 1));
    yearSpinner.setEditor(createYearSpinnerEditor(yearSpinner));

    final JComboBox<Item<Month>> monthComboBox = new JComboBox<>(ItemComboBoxModel.createModel(createMonthItems()));
    final InputMap monthComboBoxInputMap = monthComboBox.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    //so it doesn't interfere with keyboard navigation when it has focus, these two showed the popup
    monthComboBoxInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.ALT_DOWN_MASK,false), "none");
    monthComboBoxInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.ALT_DOWN_MASK,false), "none");

    final JSpinner hourSpinner = new JSpinner(new SpinnerNumberModel(hourValue.get().intValue(), 0, 23, 1));
    hourSpinner.setEditor(createTimeSpinnerEditor(hourSpinner));

    final JSpinner minuteSpinner = new JSpinner(new SpinnerNumberModel(minuteValue.get().intValue(), 0, 59, 1));
    minuteSpinner.setEditor(createTimeSpinnerEditor(minuteSpinner));

    ComponentValues.integerSpinner(yearSpinner).link(yearValue);
    ComponentValues.itemComboBox(monthComboBox).link(monthValue);
    ComponentValues.integerSpinner(hourSpinner).link(hourValue);
    ComponentValues.integerSpinner(minuteSpinner).link(minuteValue);

    final JPanel yearMonthHourMinutePanel = new JPanel(flexibleGridLayout(1, 0));
    yearMonthHourMinutePanel.add(monthComboBox);
    yearMonthHourMinutePanel.add(yearSpinner);
    if (includeTime) {
      yearMonthHourMinutePanel.add(new JLabel(" "));
      yearMonthHourMinutePanel.add(hourSpinner);
      yearMonthHourMinutePanel.add(new JLabel(":", SwingConstants.CENTER));
      yearMonthHourMinutePanel.add(minuteSpinner);
    }

    return yearMonthHourMinutePanel;
  }

  private JPanel createDayPanel() {
    final JPanel dayPanel = new JPanel(borderLayout());
    dayPanel.add(createDayHeaderPanel(), BorderLayout.NORTH);
    dayPanel.add(dayGridPanel, BorderLayout.CENTER);
    dayPanel.setBorder(BorderFactory.createTitledBorder(""));

    return dayPanel;
  }

  private void layoutDayPanel() {
    final boolean dayPanelHasFocus = dayPanelHasFocus();
    dayGridPanel.removeAll();
    final int firstDayOfMonth = LocalDate.of(yearValue.get(), monthValue.get(), 1).getDayOfWeek().getValue();
    int fieldCount = 0;
    int fillerCount = 0;
    for (int i = 1; i < firstDayOfMonth; i++) {
      dayGridPanel.add(dayFillLabels.get(fillerCount++));
      fieldCount++;
    }
    final YearMonth yearMonth = YearMonth.of(yearValue.get(), monthValue.get());
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

  private void requestCurrentDayButtonFocus() {
    dayButtons.get(dayValue.get()).requestFocusInWindow();
  }

  private void setYearMonthDay(final LocalDate localDate) {
    yearValue.set(localDate.getYear());
    monthValue.set(localDate.getMonth());
    dayValue.set(localDate.getDayOfMonth());
  }

  private void setYearMonthDayHourMinute(final LocalDateTime localDateTime) {
    setYearMonthDay(localDateTime.toLocalDate());
    hourValue.set(localDateTime.getHour());
    minuteValue.set(localDateTime.getMinute());
  }

  private LocalDateTime createLocalDateTime() {
    return LocalDateTime.of(yearValue.get(), monthValue.get(), dayValue.get(), hourValue.get(), minuteValue.get());
  }

  private void updateDateTime() {
    //prevent illegal day values
    final YearMonth yearMonth = YearMonth.of(yearValue.get(), monthValue.get());
    dayStates.get(dayValue.get() > yearMonth.lengthOfMonth() ? yearMonth.lengthOfMonth() : dayValue.get()).set(true);
    final LocalDateTime localDateTime = createLocalDateTime();
    localDateValue.set(localDateTime.toLocalDate());
    localDateTimeValue.set(localDateTime);
    SwingUtilities.invokeLater(this::updateFormattedDate);
  }

  private void updateFormattedDate() {
    formattedDateLabel.setText(dateFormatter.format(getDateTime()) + (includeTime ? " " + timeFormatter.format(getDateTime()) : ""));
  }

  private void addKeyEvents() {
    KeyEvents.builder(KeyEvent.VK_LEFT)
            .action(Control.control(this::previousYear))
            .onKeyPressed()
            .modifiers(InputEvent.SHIFT_DOWN_MASK)
            .condition(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .enable(this);
    KeyEvents.builder(KeyEvent.VK_RIGHT)
            .action(Control.control(this::nextYear))
            .onKeyPressed()
            .modifiers(InputEvent.SHIFT_DOWN_MASK)
            .condition(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .enable(this);
    KeyEvents.builder(KeyEvent.VK_LEFT)
            .action(Control.control(this::previousMonth))
            .onKeyPressed()
            .modifiers(InputEvent.CTRL_DOWN_MASK)
            .condition(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .enable(this);
    KeyEvents.builder(KeyEvent.VK_RIGHT)
            .action(Control.control(this::nextMonth))
            .onKeyPressed()
            .modifiers(InputEvent.CTRL_DOWN_MASK)
            .condition(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .enable(this);
    KeyEvents.builder(KeyEvent.VK_UP)
            .action(Control.control(this::previousWeek))
            .onKeyPressed()
            .modifiers(InputEvent.ALT_DOWN_MASK)
            .condition(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .enable(this);
    KeyEvents.builder(KeyEvent.VK_DOWN)
            .action(Control.control(this::nextWeek))
            .onKeyPressed()
            .modifiers(InputEvent.ALT_DOWN_MASK)
            .condition(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .enable(this);
    KeyEvents.builder(KeyEvent.VK_LEFT)
            .action(Control.control(this::previousDay))
            .onKeyPressed()
            .modifiers(InputEvent.ALT_DOWN_MASK)
            .condition(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .enable(this);
    KeyEvents.builder(KeyEvent.VK_RIGHT)
            .action(Control.control(this::nextDay))
            .onKeyPressed()
            .modifiers(InputEvent.ALT_DOWN_MASK)
            .condition(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .enable(this);
    if (includeTime) {
      KeyEvents.builder(KeyEvent.VK_LEFT)
              .action(Control.control(this::previousHour))
              .onKeyPressed()
              .modifiers(InputEvent.CTRL_DOWN_MASK + InputEvent.ALT_DOWN_MASK)
              .condition(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .enable(this);
      KeyEvents.builder(KeyEvent.VK_RIGHT)
              .action(Control.control(this::nextHour))
              .onKeyPressed()
              .modifiers(InputEvent.CTRL_DOWN_MASK + InputEvent.ALT_DOWN_MASK)
              .condition(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .enable(this);
      KeyEvents.builder(KeyEvent.VK_LEFT)
              .action(Control.control(this::previousMinute))
              .onKeyPressed()
              .modifiers(InputEvent.SHIFT_DOWN_MASK + InputEvent.ALT_DOWN_MASK)
              .condition(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .enable(this);
      KeyEvents.builder(KeyEvent.VK_RIGHT)
              .action(Control.control(this::nextMinute))
              .onKeyPressed()
              .modifiers(InputEvent.SHIFT_DOWN_MASK + InputEvent.ALT_DOWN_MASK)
              .condition(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
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

  private static JSpinner.NumberEditor createYearSpinnerEditor(final JSpinner spinner) {
    final JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner);
    editor.getTextField().setHorizontalAlignment(SwingConstants.CENTER);
    editor.getTextField().setColumns(YEAR_COLUMNS);
    editor.getFormat().setGroupingUsed(false);

    return editor;
  }

  private static JSpinner.NumberEditor createTimeSpinnerEditor(final JSpinner spinner) {
    final JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, "00");
    editor.getTextField().setHorizontalAlignment(SwingConstants.CENTER);
    editor.getTextField().setColumns(TIME_COLUMNS);

    return editor;
  }

  private static JPanel createDayHeaderPanel() {
    final JPanel headerPanel = new JPanel(gridLayout(1, DAYS_IN_WEEK));
    headerPanel.add(createDayLabel(DayOfWeek.MONDAY));
    headerPanel.add(createDayLabel(DayOfWeek.TUESDAY));
    headerPanel.add(createDayLabel(DayOfWeek.WEDNESDAY));
    headerPanel.add(createDayLabel(DayOfWeek.THURSDAY));
    headerPanel.add(createDayLabel(DayOfWeek.FRIDAY));
    headerPanel.add(createDayLabel(DayOfWeek.SATURDAY));
    headerPanel.add(createDayLabel(DayOfWeek.SUNDAY));

    return headerPanel;
  }

  private static JLabel createDayLabel(final DayOfWeek dayOfWeek) {
    final JLabel label = new JLabel(dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()), SwingConstants.CENTER);
    label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    return label;
  }

  private static List<Item<Month>> createMonthItems() {
    final List<Item<Month>> months = new ArrayList<>(MONTHS_IN_YEAR);
    Arrays.stream(Month.values()).forEach(month ->
            months.add(Item.item(month, month.getDisplayName(TextStyle.FULL, Locale.getDefault()))));

    return months;
  }
}
