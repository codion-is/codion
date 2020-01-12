/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui;

import org.jminor.common.state.StateObserver;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.textfield.TextFields;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import java.awt.BorderLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * A panel for displaying a formatted text field for date/time input.
 */
public final class LocalDateTimeInputPanel extends TemporalInputPanel<LocalDateTime> {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(LocalDateInputPanel.class.getName(), Locale.getDefault());

  private JButton button;

  /**
   * Instantiates a new LocalDateTimeInputPanel.
   * @param initialValue the initial value to display
   * @param dateFormat the date format
   */
  public LocalDateTimeInputPanel(final LocalDateTime initialValue, final String dateFormat) {
    this(TextFields.createFormattedTemporalField(dateFormat, initialValue), dateFormat, true, null);
  }

  /**
   * Instantiates a new LocalDateTimeInputPanel.
   * @param inputField the input field
   * @param includeCalendarButton if true and JCalendar is available, a button for displaying a calendar is included
   * @param dateFormat the date format
   * @param enabledState a StateObserver controlling the enabled state of the input field
   */
  public LocalDateTimeInputPanel(final JFormattedTextField inputField, final String dateFormat,
                                 final boolean includeCalendarButton, final StateObserver enabledState) {
    super(inputField, dateFormat, LocalDateTime::parse, enabledState);
    if (includeCalendarButton && UiUtil.isJCalendarAvailable()) {
      this.button = new JButton(Controls.control(this::displayCalendar, "..."));
      this.button.setPreferredSize(TextFields.DIMENSION_TEXT_FIELD_SQUARE);
      if (enabledState != null) {
        UiUtil.linkToEnabledState(enabledState, button);
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

  /** {@inheritDoc} */
  @Override
  public void setEditable(final boolean editable) {
    super.setEditable(editable);
    if (button != null) {
      button.setEnabled(editable);
    }
  }

  private void displayCalendar() {
    LocalDateTime currentValue = null;
    try {
      currentValue = getTemporal();
    }
    catch (final DateTimeParseException ignored) {/*ignored*/}
    final JFormattedTextField inputField = getInputField();
    final LocalDate newValue = UiUtil.getDateWithCalendar(currentValue == null ? null : currentValue.toLocalDate(),
            MESSAGES.getString("select_date"), inputField);
    if (newValue != null) {
      inputField.setText(getFormatter().format(newValue.atStartOfDay()));
      final String inputText = inputField.getText();
      final int colonIndex = inputText.lastIndexOf(':');
      if (colonIndex != -1) {
        inputField.setCaretPosition(colonIndex - 2);
        inputField.moveCaretPosition(colonIndex + 3);
      }
      inputField.requestFocusInWindow();
    }
  }
}
