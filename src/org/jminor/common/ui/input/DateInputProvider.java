/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.input;

import org.jminor.common.ui.DateInputPanel;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A InputProvider implementation for date values.
 */
public final class DateInputProvider extends AbstractInputProvider<Date, DateInputPanel> {

  public DateInputProvider(final Date currentValue, final SimpleDateFormat dateFormat) {
    super(new DateInputPanel(currentValue, dateFormat));
  }

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
    catch (ParseException e) {
      throw new RuntimeException("Wrong date format " + getInputComponent().getFormatPattern() + " expected", e);
    }
  }
}
