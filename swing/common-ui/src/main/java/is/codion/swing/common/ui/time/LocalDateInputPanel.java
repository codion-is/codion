/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.common.ui.time;

import dev.codion.common.event.Event;
import dev.codion.common.event.Events;
import dev.codion.common.i18n.Messages;
import dev.codion.common.state.State;
import dev.codion.common.state.StateObserver;
import dev.codion.common.state.States;
import dev.codion.swing.common.ui.Components;
import dev.codion.swing.common.ui.KeyEvents;
import dev.codion.swing.common.ui.control.Control;
import dev.codion.swing.common.ui.control.Controls;
import dev.codion.swing.common.ui.dialog.Dialogs;
import dev.codion.swing.common.ui.dialog.DisposeOnEscape;
import dev.codion.swing.common.ui.dialog.Modal;
import dev.codion.swing.common.ui.layout.Layouts;
import dev.codion.swing.common.ui.textfield.TextFields;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.KeyEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.ResourceBundle;

/**
 * A panel for LocalDate input via a formatted text field and a button activating a calendar for date input.
 */
public final class LocalDateInputPanel extends TemporalInputPanel<LocalDate> {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(LocalDateInputPanel.class.getName());

  private JButton button;

  /**
   * Instantiates a new LocalDateInputPanel.
   * @param initialValue the initial value to display
   * @param dateFormat the date format
   */
  public LocalDateInputPanel(final LocalDate initialValue, final String dateFormat) {
    this(TextFields.createFormattedTemporalField(dateFormat, initialValue), dateFormat, CalendarButton.YES, null);
  }

  /**
   * Instantiates a new LocalTimeInputPanel.
   * @param inputField the input field
   * @param calendarButton if yes and JCalendar is available, a button for displaying a calendar is included
   * @param dateFormat the date format
   * @param enabledState a StateObserver controlling the enabled state of the input field
   */
  public LocalDateInputPanel(final JFormattedTextField inputField, final String dateFormat,
                             final CalendarButton calendarButton, final StateObserver enabledState) {
    super(inputField, dateFormat, LocalDate::parse, enabledState);
    if (calendarButton == CalendarButton.YES && TemporalInputPanel.isJCalendarAvailable()) {
      this.button = new JButton(Controls.control(this::displayCalendar, "..."));
      this.button.setPreferredSize(TextFields.DIMENSION_TEXT_FIELD_SQUARE);
      if (enabledState != null) {
        Components.linkToEnabledState(enabledState, button);
      }
      add(this.button, BorderLayout.EAST);
    }
  }

  /**
   * @return the button, if any
   */
  public JButton getCalendarButton() {
    return button;
  }

  @Override
  public void setEditable(final boolean editable) {
    super.setEditable(editable);
    if (button != null) {
      button.setEnabled(editable);
    }
  }

  /**
   * Retrieves a LocalDate from the user via JCalendar.
   * @param startDate the starting date, if null the current date is used
   * @param message the message to display as dialog title
   * @param parent the dialog parent
   * @return a LocalDate from the user, null if the action was cancelled
   * @throws IllegalStateException in case JCalendar is not found on the classpath
   */
  public static LocalDate getDateWithCalendar(final LocalDate startDate, final String message, final Container parent) {
    if (!TemporalInputPanel.isJCalendarAvailable()) {
      throw new IllegalStateException("JCalendar library is not available");
    }

    try {
      final Calendar cal = Calendar.getInstance();
      cal.setTime(startDate == null ? Date.from(Instant.now()) : Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));

      final Class jCalendarClass = Class.forName(JCALENDAR_CLASS_NAME);
      final Method getCalendar = jCalendarClass.getMethod("getCalendar");
      final Constructor constructor = jCalendarClass.getConstructor(Calendar.class);
      final JPanel calendarPanel = (JPanel) constructor.newInstance(cal);
      final JPanel datePanel = new JPanel(Layouts.borderLayout());
      datePanel.add(calendarPanel, BorderLayout.NORTH);

      final Event closeEvent = Events.event();
      final State cancel = States.state();
      final Calendar returnTime = Calendar.getInstance();
      returnTime.setTime(cal.getTime());
      final Control okControl = Controls.control(() -> {
        returnTime.setTimeInMillis(((Calendar) getCalendar.invoke(calendarPanel)).getTimeInMillis());
        closeEvent.onEvent();
      }, Messages.get(Messages.OK));
      final JButton okButton = new JButton(okControl);
      final Control cancelControl = Controls.control(() -> {
        cancel.set(true);
        closeEvent.onEvent();
      }, Messages.get(Messages.CANCEL));
      final JButton cancelButton = new JButton(cancelControl);
      final JPanel buttonPanel = new JPanel(Layouts.gridLayout(1, 2));
      buttonPanel.add(okButton);
      buttonPanel.add(cancelButton);

      datePanel.add(buttonPanel, BorderLayout.SOUTH);

      KeyEvents.addKeyEvent(datePanel, KeyEvent.VK_ESCAPE, 0, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, cancelControl);
      Dialogs.displayInDialog(parent, datePanel, message, Modal.YES, okControl, closeEvent, DisposeOnEscape.YES);

      return cancel.get() ? null : Instant.ofEpochMilli(returnTime.getTime().getTime())
              .atZone(ZoneId.systemDefault())
              .toLocalDate();
    }
    catch (final Exception e) {
      throw new RuntimeException("Exception while using JCalendar", e);
    }
  }

  private void displayCalendar() {
    LocalDate currentValue = null;
    try {
      currentValue = getTemporal();
    }
    catch (final DateTimeParseException ignored) {/*ignored*/}
    final JFormattedTextField inputField = getInputField();
    final LocalDate newValue = getDateWithCalendar(currentValue, MESSAGES.getString("select_date"), inputField);
    if (newValue != null) {
      inputField.setText(getFormatter().format(newValue));
      inputField.requestFocusInWindow();
    }
  }
}
