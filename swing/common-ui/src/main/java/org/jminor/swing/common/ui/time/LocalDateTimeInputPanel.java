/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.time;

import org.jminor.common.DateFormats;
import org.jminor.common.state.StateObserver;
import org.jminor.swing.common.ui.Components;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.dialog.Dialogs;
import org.jminor.swing.common.ui.textfield.TextFields;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.text.MaskFormatter;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ResourceBundle;

/**
 * A panel for displaying a formatted text field for date/time input.
 */
public final class LocalDateTimeInputPanel extends TemporalInputPanel<LocalDateTime> {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(LocalDateInputPanel.class.getName());

  private static final int DEFAULT_DATE_FIELD_COLUMNS = 12;

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
    if (includeCalendarButton && TemporalInputPanel.isJCalendarAvailable()) {
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

  /** {@inheritDoc} */
  @Override
  public void setEditable(final boolean editable) {
    super.setEditable(editable);
    if (button != null) {
      button.setEnabled(editable);
    }
  }

  /**
   * Retrieves a date from the user using a simple formatted text field
   * @param startDate the initial date, if null the current date is used
   * @param message the message to display as dialog title
   * @param dateFormat the date format to use
   * @param parent the dialog parent
   * @return a LocalDateTime from the user
   */
  public static LocalDateTime getDateTimeFromUserAsText(final LocalDateTime startDate, final String message,
                                                        final String dateFormat, final Container parent) {
    try {
      final MaskFormatter formatter = new MaskFormatter(DateFormats.getDateMask(dateFormat));
      formatter.setPlaceholderCharacter('_');
      final JFormattedTextField textField = new JFormattedTextField(new SimpleDateFormat(dateFormat));
      textField.setColumns(DEFAULT_DATE_FIELD_COLUMNS);
      textField.setValue(startDate);

      final JPanel datePanel = new JPanel(new GridLayout(1, 1));
      datePanel.add(textField);

      Dialogs.displayInDialog(parent, datePanel, message);

      return LocalDateTime.parse(textField.getText(), DateTimeFormatter.ofPattern(dateFormat));
    }
    catch (final ParseException e) {
      throw new RuntimeException(e);
    }
  }

  private void displayCalendar() {
    LocalDateTime currentValue = null;
    try {
      currentValue = getTemporal();
    }
    catch (final DateTimeParseException ignored) {/*ignored*/}
    final JFormattedTextField inputField = getInputField();
    final LocalDate newValue = LocalDateInputPanel.getDateWithCalendar(currentValue == null ? null : currentValue.toLocalDate(),
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
