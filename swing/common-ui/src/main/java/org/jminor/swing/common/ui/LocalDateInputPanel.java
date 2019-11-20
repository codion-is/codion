/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui;

import org.jminor.common.state.StateObserver;
import org.jminor.swing.common.ui.control.Controls;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import java.awt.BorderLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * A panel for LocalDate input via a formatted text field and a button activating a calendar for date input.
 */
public final class LocalDateInputPanel extends TemporalInputPanel<LocalDate> {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(LocalDateInputPanel.class.getName(), Locale.getDefault());

  private JButton button;

  /**
   * Instantiates a new LocalDateInputPanel.
   * @param initialValue the initial value to display
   * @param dateFormat the date format
   */
  public LocalDateInputPanel(final LocalDate initialValue, final String dateFormat) {
    this(UiUtil.createFormattedTemporalField(dateFormat, initialValue), dateFormat, true, null);
  }

  /**
   * Instantiates a new LocalTimeInputPanel.
   * @param inputField the input field
   * @param includeCalendarButton if true and JCalendar is available, a button for displaying a calendar is included
   * @param dateFormat the date format
   * @param enabledState a StateObserver controlling the enabled state of the input field
   */
  public LocalDateInputPanel(final JFormattedTextField inputField, final String dateFormat,
                             final boolean includeCalendarButton, final StateObserver enabledState) {
    super(inputField, dateFormat, LocalDate::parse, enabledState);
    if (includeCalendarButton && UiUtil.isJCalendarAvailable()) {
      this.button = new JButton(Controls.control(this::displayCalendar, "..."));
      this.button.setPreferredSize(UiUtil.DIMENSION_TEXT_FIELD_SQUARE);
      if (enabledState != null) {
        UiUtil.linkToEnabledState(enabledState, button);
      }
      add(this.button, BorderLayout.EAST);
    }
  }

  /**
   * @return the button, if any
   */
  public final JButton getCalendarButton() {
    return button;
  }

  /** {@inheritDoc} */
  @Override
  public final void setEditable(final boolean editable) {
    super.setEditable(editable);
    if (button != null) {
      button.setEnabled(editable);
    }
  }

  private void displayCalendar() {
    LocalDate currentValue = null;
    try {
      currentValue = getTemporal();
    }
    catch (final DateTimeParseException ignored) {/*ignored*/}
    final LocalDate newValue = UiUtil.getDateWithCalendar(currentValue, MESSAGES.getString("select_date"), getInputField());
    if (newValue != null) {
      getInputField().setText(getFormatter().format(newValue));
    }
  }
}
