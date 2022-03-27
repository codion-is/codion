/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.textfield.TemporalField;

import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;

final class TemporalFieldValue<T extends Temporal> extends FormattedTextComponentValue<T, TemporalField<T>> {

  TemporalFieldValue(TemporalField<T> textComponent, UpdateOn updateOn) {
    super(textComponent, null, updateOn);
  }

  @Override
  protected String formatTextFromValue(T value) {
    return getComponent().getDateTimeFormatter().format(value);
  }

  @Override
  protected T parseValueFromText(String text) {
    try {
      return getComponent().getDateTimeParser().parse(text, getComponent().getDateTimeFormatter());
    }
    catch (DateTimeParseException e) {
      return null;
    }
  }
}
