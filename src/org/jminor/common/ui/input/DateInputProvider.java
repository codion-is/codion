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
 * A InputManager implementation for date values.
 */
public class DateInputProvider extends AbstractInputProvider<Date> {

  public DateInputProvider(final Date currentValue, final SimpleDateFormat dateFormat) {
    super(new DateInputPanel(currentValue, dateFormat));
  }

  @Override
  public Date getValue() {
    try {
      final String dateText = ((DateInputPanel) getInputComponent()).getInputField().getText();
      if (!dateText.contains("_"))
        return new Timestamp(((DateInputPanel) getInputComponent()).getDate().getTime());
      else
        return null;
    }
    catch (ParseException e) {
      throw new RuntimeException("Wrong date format "
              + ((DateInputPanel) getInputComponent()).getFormatPattern() + " expected");
    }
  }
}
