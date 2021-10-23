/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.swing.common.ui.textfield.TemporalField;

import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;

final class TemporalFieldValue<T extends Temporal> extends FormattedTextComponentValue<T, TemporalField<T>> {

  TemporalFieldValue(final TemporalField<T> textComponent, final UpdateOn updateOn) {
    super(textComponent, null, updateOn);
  }

  @Override
  protected String formatTextFromValue(final T value) {
    return getComponent().getDateTimeFormatter().format(value);
  }

  @Override
  protected T parseValueFromText(final String text) {
    try {
      return getComponent().getDateTimeParser().parse(text, getComponent().getDateTimeFormatter());
    }
    catch (final DateTimeParseException e) {
      return null;
    }
  }
}
