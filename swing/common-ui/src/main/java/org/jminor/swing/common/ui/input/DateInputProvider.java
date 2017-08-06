/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.input;

import org.jminor.swing.common.ui.DateInputPanel;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A InputProvider implementation for date values.
 */
public final class DateInputProvider extends AbstractInputProvider<Date, DateInputPanel> {

  /**
   * Instantiates a new DateInputProvider.
   * @param initialValue the initial value
   * @param dateFormat the date format
   */
  public DateInputProvider(final Date initialValue, final SimpleDateFormat dateFormat) {
    super(new DateInputPanel(initialValue, dateFormat));
  }

  /** {@inheritDoc} */
  @Override
  public Date getValue() {
    try {
      final String dateText = getInputComponent().getInputField().getText();
      if (dateText.length() == 0) {
        return null;
      }
      if (!dateText.contains("_")) {
        return new Timestamp(getInputComponent().getDate().getTime());
      }
      else {
        return null;
      }
    }
    catch (final ParseException e) {
      throw new IllegalArgumentException("Wrong date format " + getInputComponent().getFormatPattern() + " expected", e);
    }
  }
}
