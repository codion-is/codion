/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.time;

import is.codion.common.state.StateObserver;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.textfield.TemporalField;
import is.codion.swing.common.ui.textfield.TextFields;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import java.awt.BorderLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ResourceBundle;

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
      this.button = Control.builder(this::displayCalendar)
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
  @Override
  public JButton getCalendarButton() {
    return button;
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
