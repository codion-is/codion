/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.common.ui.time;

import dev.codion.common.state.StateObserver;
import dev.codion.swing.common.ui.textfield.TextFields;

import javax.swing.JFormattedTextField;
import java.time.LocalTime;

/**
 * A panel for displaying a formatted text field and a button activating a calendar for date input.
 */
public final class LocalTimeInputPanel extends TemporalInputPanel<LocalTime> {

  /**
   * Instantiates a new LocalTimeInputPanel.
   * @param initialValue the initial value to display
   * @param dateFormat the date format
   */
  public LocalTimeInputPanel(final LocalTime initialValue, final String dateFormat) {
    this(TextFields.createFormattedTemporalField(dateFormat, initialValue), dateFormat, null);
  }

  /**
   * Instantiates a new LocalTimeInputPanel.
   * @param inputField the input field
   * @param dateFormat the date format
   * @param enabledState a StateObserver controlling the enabled state of the input field
   */
  public LocalTimeInputPanel(final JFormattedTextField inputField, final String dateFormat, final StateObserver enabledState) {
    super(inputField, dateFormat, LocalTime::parse, enabledState);
  }
}
