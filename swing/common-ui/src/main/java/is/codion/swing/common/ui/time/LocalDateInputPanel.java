/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.time;

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

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ResourceBundle;

import static is.codion.swing.common.ui.Components.createOkCancelButtonPanel;

/**
 * A panel for LocalDate input via a formatted text field and a button activating a calendar for date input.
 */
final class LocalDateInputPanel extends TemporalInputPanel<LocalDate> {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(LocalDateInputPanel.class.getName());

  private JButton button;

  /**
   * Instantiates a new LocalTimeInputPanel.
   * @param inputField the input field
   * @param calendarButton if true a button for displaying a calendar is included
   * @param enabledState a StateObserver controlling the enabled state of the input field
   */
  LocalDateInputPanel(final TemporalField<LocalDate> inputField, final boolean calendarButton, final StateObserver enabledState) {
    super(inputField, enabledState);
    if (calendarButton) {
      this.button = Control.builder()
              .command(this::displayCalendar)
              .name("...")
              .build().createButton();
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

  /**
   * Retrieves a LocalDate from the user.
   * @param startDate the starting date, if null the current date is used
   * @param message the message to display as dialog title
   * @param parent the dialog parent
   * @return a LocalDate from the user, null if the action was cancelled
   */
  public static LocalDate getLocalDateWithCalendar(final LocalDate startDate, final String message, final Container parent) {
    final Event<?> closeEvent = Event.event();
    final State cancel = State.state();
    final Control okControl = Control.control(closeEvent::onEvent);
    final Control cancelControl = Control.control(() -> {
      cancel.set(true);
      closeEvent.onEvent();
    });

    final CalendarPanel calendarPanel = new CalendarPanel();
    calendarPanel.setSelectedDate(startDate);
    final JPanel datePanel = new JPanel(Layouts.borderLayout());
    datePanel.add(calendarPanel, BorderLayout.NORTH);
    datePanel.add(createOkCancelButtonPanel(okControl, cancelControl), BorderLayout.SOUTH);

    KeyEvents.builder()
            .keyEvent(KeyEvent.VK_ESCAPE)
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .action(cancelControl)
            .enable(datePanel);
    Dialogs.displayInDialog(parent, datePanel, message, Modal.YES, okControl, closeEvent, DisposeOnEscape.YES);

    return cancel.get() ? null : calendarPanel.getSelectedDate();
  }

  private void displayCalendar() {
    LocalDate currentValue = null;
    try {
      currentValue = getTemporal();
    }
    catch (final DateTimeParseException ignored) {/*ignored*/}
    final JFormattedTextField inputField = getInputField();
    final LocalDate newValue = getLocalDateWithCalendar(currentValue, MESSAGES.getString("select_date"), inputField);
    if (newValue != null) {
      inputField.setText(getInputField().getDateTimeFormatter().format(newValue));
      inputField.requestFocusInWindow();
    }
  }
}
