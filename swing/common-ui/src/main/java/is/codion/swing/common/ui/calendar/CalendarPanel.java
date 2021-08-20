/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.calendar;

import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.value.ComponentValues;

import javax.swing.BorderFactory;
import javax.swing.FocusManager;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
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
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static is.codion.swing.common.ui.layout.Layouts.*;
import static java.util.Objects.requireNonNull;

/**
 * A panel presenting a calendar for date/time selection.<br><br>
 * Select previous/next month with CTRL + left/right arrow.<br>
 * Select previous/next year with ALT + left/right arrow.
 */
public final class CalendarPanel extends JPanel {

  private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL);

  private final Value<Integer> yearValue;
  private final Value<Integer> monthValue;
  private final Value<Integer> dayValue;
  private final Value<Integer> hourValue;
  private final Value<Integer> minuteValue;

  private final Map<Integer, JToggleButton> dayButtons;
  private final Map<Integer, State> dayStates;
  private final JPanel dayGridPanel;
  private final JLabel formattedDateLabel;

  /**
   * Instantiates a new {@link CalendarPanel} without time fields.
   */
  public CalendarPanel() {
    this(false);
  }

  /**
   * Instantiates a new {@link CalendarPanel}.
   * @param includeTime true if time fields (hours/minutes) should be included
   */
  public CalendarPanel(final boolean includeTime) {
    final LocalDateTime dateTime = LocalDateTime.now();
    yearValue = Value.value(dateTime.getYear(), dateTime.getYear());
    monthValue = Value.value(dateTime.getMonthValue(), dateTime.getMonthValue());
    dayValue = Value.value(dateTime.getDayOfMonth(), dateTime.getDayOfMonth());
    hourValue = Value.value(dateTime.getHour(), dateTime.getHour());
    minuteValue = Value.value(dateTime.getMinute(), dateTime.getMinute());
    dayStates = createDayStates();
    dayButtons = createDayButtons();
    dayGridPanel = new JPanel(gridLayout(6, 7));
    formattedDateLabel = new JLabel(dateFormatter.format(getDateTime()), SwingConstants.CENTER);
    initializeUI(includeTime);
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
   * Sets the date/time to present in this calendar
   * @param dateTime the date/time to set
   */
  public void setDateTime(final LocalDateTime dateTime) {
    requireNonNull(dateTime);
    yearValue.set(dateTime.getYear());
    monthValue.set(dateTime.getMonthValue());
    dayValue.set(dateTime.getDayOfMonth());
    hourValue.set(dateTime.getHour());
    minuteValue.set(dateTime.getMinute());
  }

  /**
   * @return the date/time currently displayed in this calendar
   */
  public LocalDateTime getDateTime() {
    return LocalDateTime.of(yearValue.get(), monthValue.get(), dayValue.get(), hourValue.get(), minuteValue.get());
  }

  private Map<Integer, State> createDayStates() {
    final Map<Integer, State> states = new HashMap<>();
    final State.Group stateGroup = State.group();
    for (int dayOfMonth = 1; dayOfMonth <= 31; dayOfMonth++) {
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

  private void initializeUI(final boolean includeTime) {
    setLayout(borderLayout());
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    add(createNorthPanel(), BorderLayout.NORTH);
    add(createDayPanel(), BorderLayout.CENTER);
    if (includeTime) {
      add(createTimePanel(), BorderLayout.SOUTH);
    }
    layoutDayPanel();
  }

  private JPanel createNorthPanel() {
    final JPanel previousYearMonthPanel = new JPanel(gridLayout(1, 2));
    previousYearMonthPanel.add(createPreviousYearButton());
    previousYearMonthPanel.add(createPreviousMonthButton());

    final JPanel nextYearMonthPanel = new JPanel(gridLayout(1, 2));
    nextYearMonthPanel.add(createNextMonthButton());
    nextYearMonthPanel.add(createNextYearButton());

    final JPanel northPanel = new JPanel(borderLayout());
    northPanel.add(formattedDateLabel, BorderLayout.NORTH);
    northPanel.add(previousYearMonthPanel, BorderLayout.WEST);
    northPanel.add(nextYearMonthPanel, BorderLayout.EAST);

    return northPanel;
  }

  private JButton createPreviousYearButton() {
    final Control previousYearControl = Control.builder(this::previousYear)
            .caption("<<")
            .build();
    KeyEvents.builder(KeyEvent.VK_LEFT)
            .action(previousYearControl)
            .modifiers(InputEvent.CTRL_DOWN_MASK)
            .condition(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .enable(this);

    return previousYearControl.createButton();
  }

  private JButton createPreviousMonthButton() {
    final Control previousMonthControl = Control.builder(this::previousMonth)
            .caption("<")
            .build();
    KeyEvents.builder(KeyEvent.VK_LEFT)
            .action(previousMonthControl)
            .modifiers(InputEvent.ALT_DOWN_MASK)
            .condition(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .enable(this);

    return previousMonthControl.createButton();
  }

  private JButton createNextMonthButton() {
    final Control nextMonthControl = Control.builder(this::nextMonth)
            .caption(">")
            .build();
    KeyEvents.builder(KeyEvent.VK_RIGHT)
            .action(nextMonthControl)
            .modifiers(InputEvent.ALT_DOWN_MASK)
            .condition(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .enable(this);

    return nextMonthControl.createButton();
  }

  private JButton createNextYearButton() {
    final Control nextYearControl = Control.builder(this::nextYear)
            .caption(">>")
            .build();
    KeyEvents.builder(KeyEvent.VK_RIGHT)
            .action(nextYearControl)
            .modifiers(InputEvent.CTRL_DOWN_MASK)
            .condition(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .enable(this);

    return nextYearControl.createButton();
  }

  private JPanel createDayPanel() {
    final JPanel dayPanel = new JPanel(borderLayout());
    dayPanel.add(createDayHeaderPanel(), BorderLayout.NORTH);
    dayPanel.add(dayGridPanel, BorderLayout.CENTER);

    return dayPanel;
  }

  private JPanel createTimePanel() {
    final JSpinner hourSpinner = new JSpinner(new SpinnerNumberModel(hourValue.get().intValue(), 0, 23, 1));
    final JSpinner minuteSpinner = new JSpinner(new SpinnerNumberModel(minuteValue.get().intValue(), 0, 59, 1));

    ComponentValues.integerSpinner(hourSpinner).link(hourValue);
    ComponentValues.integerSpinner(minuteSpinner).link(minuteValue);

    final JPanel hourMinutePanel = new JPanel(gridLayout(1, 2));
    hourMinutePanel.add(hourSpinner);
    hourMinutePanel.add(minuteSpinner);

    final JPanel panel = new JPanel(flowLayout(FlowLayout.CENTER));
    panel.add(hourMinutePanel);

    return panel;
  }

  private void previousMonth() {
    final LocalDate previousMonth = getDate().minus(1, ChronoUnit.MONTHS);
    monthValue.set(previousMonth.getMonthValue());
    yearValue.set(previousMonth.getYear());
  }

  private void nextMonth() {
    final LocalDate nextMonth = getDate().plus(1, ChronoUnit.MONTHS);
    monthValue.set(nextMonth.getMonthValue());
    yearValue.set(nextMonth.getYear());
  }

  private void previousYear() {
    yearValue.set(yearValue.get() - 1);
  }

  private void nextYear() {
    yearValue.set(yearValue.get() + 1);
  }

  private void bindEvents() {
    yearValue.addListener(this::updateDate);
    monthValue.addListener(this::updateDate);
    dayValue.addListener(this::updateDate);
    yearValue.addListener(() -> SwingUtilities.invokeLater(this::layoutDayPanel));
    monthValue.addListener(() -> SwingUtilities.invokeLater(this::layoutDayPanel));
  }

  private void layoutDayPanel() {
    final boolean requestFocusAfterLayout = dayGridPanel.isAncestorOf(FocusManager.getCurrentManager().getFocusOwner());
    dayGridPanel.removeAll();
    final int firstDayOfMonth = LocalDate.of(yearValue.get(), monthValue.get(), 1).getDayOfWeek().getValue();
    int fieldCount = 0;
    for (int i = 1; i < firstDayOfMonth; i++) {
      dayGridPanel.add(new JLabel());
      fieldCount++;
    }
    final YearMonth yearMonth = YearMonth.of(yearValue.get(), monthValue.get());
    for (int dayOfMonth = 1; dayOfMonth <= yearMonth.lengthOfMonth(); dayOfMonth++) {
      dayGridPanel.add(dayButtons.get(dayOfMonth));
      fieldCount++;
    }
    while (fieldCount++ < 42) {
      dayGridPanel.add(new JLabel());
    }
    revalidate();
    repaint();
    if (requestFocusAfterLayout) {
      dayButtons.get(dayValue.get()).requestFocusInWindow();
    }
  }

  private void updateDate() {
    //prevent illegal day values
    final YearMonth yearMonth = YearMonth.of(yearValue.get(), monthValue.get());
    dayStates.get(dayValue.get() > yearMonth.lengthOfMonth() ? yearMonth.lengthOfMonth() : dayValue.get()).set(true);
    SwingUtilities.invokeLater(() -> formattedDateLabel.setText(dateFormatter.format(getDateTime())));
  }

  private static JPanel createDayHeaderPanel() {
    final JPanel headerPanel = new JPanel(gridLayout(1, 7));
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
    return new JLabel(dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()), SwingConstants.CENTER);
  }
}
