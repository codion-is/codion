/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui;

import org.jminor.common.StateObserver;

import javax.swing.JFormattedTextField;
import java.time.LocalDateTime;

/**
 * A panel for displaying a formatted text field for date/time input.
 */
public final class LocalDateTimeInputPanel extends TemporalInputPanel<LocalDateTime> {

  /**
   * Instantiates a new LocalDateTimeInputPanel.
   * @param initialValue the initial value to display
   * @param dateFormat the date format
   */
  public LocalDateTimeInputPanel(final LocalDateTime initialValue, final String dateFormat) {
    this(UiUtil.createFormattedTemporalField(dateFormat, initialValue), dateFormat, null);
  }

  /**
   * Instantiates a new LocalDateTimeInputPanel.
   * @param inputField the input field
   * @param dateFormat the date format
   * @param enabledState a StateObserver controlling the enabled state of the input field
   */
  public LocalDateTimeInputPanel(final JFormattedTextField inputField, final String dateFormat, final StateObserver enabledState) {
    super(inputField, dateFormat, LocalDateTime::parse, enabledState);
  }
}
