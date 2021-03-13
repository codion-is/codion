/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.time;

import is.codion.common.formats.LocaleDateTimePattern;
import is.codion.common.state.StateObserver;
import is.codion.swing.common.ui.textfield.TextFields;

import javax.swing.JFormattedTextField;
import java.time.LocalTime;

/**
 * A panel for displaying a formatted text field and a button activating a calendar for time input.
 */
public final class LocalTimeInputPanel extends TemporalInputPanel<LocalTime> {

  /**
   * Instantiates a new LocalTimeInputPanel.
   * @param initialValue the initial value to display
   * @param timePattern the time format pattern
   */
  public LocalTimeInputPanel(final LocalTime initialValue, final String timePattern) {
    this(TextFields.createFormattedField(LocaleDateTimePattern.getMask(timePattern)), timePattern, null);
    setTemporal(initialValue);
  }

  /**
   * Instantiates a new LocalTimeInputPanel.
   * @param inputField the input field
   * @param timePattern the time format pattern
   * @param enabledState a StateObserver controlling the enabled state of the input field
   */
  public LocalTimeInputPanel(final JFormattedTextField inputField, final String timePattern, final StateObserver enabledState) {
    super(inputField, timePattern, LocalTime::parse, enabledState);
  }
}
