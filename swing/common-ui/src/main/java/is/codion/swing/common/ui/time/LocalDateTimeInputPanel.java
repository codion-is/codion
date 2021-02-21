/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.time;

import is.codion.common.DateFormats;
import is.codion.common.event.Event;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.dialog.DisposeOnEscape;
import is.codion.swing.common.ui.dialog.Modal;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.common.ui.textfield.TextFields;

import com.github.lgooddatepicker.components.CalendarPanel;
import com.github.lgooddatepicker.components.TimePicker;
import com.github.lgooddatepicker.components.TimePickerSettings;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ResourceBundle;

import static is.codion.swing.common.ui.Components.createOkCancelButtonPanel;

/**
 * A panel for displaying a formatted text field for date/time input.
 */
public final class LocalDateTimeInputPanel extends TemporalInputPanel<LocalDateTime> {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(LocalDateTimeInputPanel.class.getName());

  private JButton button;

  /**
   * Instantiates a new LocalDateTimeInputPanel.
   * @param initialValue the initial value to display
   * @param dateFormat the date format
   */
  public LocalDateTimeInputPanel(final LocalDateTime initialValue, final String dateFormat) {
    this(TextFields.createFormattedField(DateFormats.getDateMask(dateFormat)), dateFormat, CalendarButton.YES, null);
    setTemporal(initialValue);
  }

  /**
   * Instantiates a new LocalDateTimeInputPanel.
   * @param inputField the input field
   * @param calendarButton if true a button for displaying a calendar is included
   * @param dateFormat the date format
   * @param enabledState a StateObserver controlling the enabled state of the input field
   */
  public LocalDateTimeInputPanel(final JFormattedTextField inputField, final String dateFormat,
                                 final CalendarButton calendarButton, final StateObserver enabledState) {
    super(inputField, dateFormat, LocalDateTime::parse, enabledState);
    if (calendarButton == CalendarButton.YES) {
      this.button = new JButton(Control.builder().command(this::displayCalendar).name("...").build());
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
   * Retrieves a LocalDateTime from the user.
   * @param startDateTime the starting date, if null the current date is used
   * @param message the message to display as dialog title
   * @param parent the dialog parent
   * @return a LocalDateTime from the user, null if the action was cancelled
   */
  public static LocalDateTime getLocalDateTimeWithCalendar(final LocalDateTime startDateTime, final String message, final Container parent) {
    final Event<?> closeEvent = Event.event();
    final State cancel = State.state();
    final Control okControl = Control.control(closeEvent::onEvent);
    final Control cancelControl = Control.control(() -> {
      cancel.set(true);
      closeEvent.onEvent();
    });

    final TimePickerSettings timeSettings = new TimePickerSettings();
    timeSettings.use24HourClockFormat();
    timeSettings.setDisplaySpinnerButtons(true);
    timeSettings.setInitialTimeToNow();
    final TimePicker timePicker = new TimePicker(timeSettings);
    final CalendarPanel calendarPanel = new CalendarPanel();
    if (startDateTime != null) {
      calendarPanel.setSelectedDate(startDateTime.toLocalDate());
      timePicker.setTime(startDateTime.toLocalTime());
    }
    final JPanel dateTimePanel = new JPanel(Layouts.borderLayout());
    final JPanel timePanel = new JPanel(new GridBagLayout());
    timePanel.add(timePicker);
    dateTimePanel.add(calendarPanel, BorderLayout.CENTER);
    dateTimePanel.add(timePanel, BorderLayout.EAST);
    dateTimePanel.add(createOkCancelButtonPanel(okControl, cancelControl), BorderLayout.SOUTH);

    KeyEvents.addKeyEvent(dateTimePanel, KeyEvent.VK_ESCAPE, 0, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, cancelControl);
    Dialogs.displayInDialog(parent, dateTimePanel, message, Modal.YES, okControl, closeEvent, DisposeOnEscape.YES);

    return cancel.get() ? null : LocalDateTime.of(calendarPanel.getSelectedDate(), timePicker.getTime());
  }

  private void displayCalendar() {
    LocalDateTime currentValue = null;
    try {
      currentValue = getTemporal();
    }
    catch (final DateTimeParseException ignored) {/*ignored*/}
    final JFormattedTextField inputField = getInputField();
    final LocalDateTime newValue = getLocalDateTimeWithCalendar(currentValue, MESSAGES.getString("select_date_time"), inputField);
    if (newValue != null) {
      inputField.setText(getFormatter().format(newValue));
      inputField.requestFocusInWindow();
    }
  }
}
