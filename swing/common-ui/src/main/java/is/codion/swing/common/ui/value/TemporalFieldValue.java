/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.swing.common.ui.time.TemporalField;

import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;

final class TemporalFieldValue<V extends Temporal> extends FormattedTextComponentValue<V, TemporalField<V>> {

  TemporalFieldValue(final TemporalField<V> textComponent, final UpdateOn updateOn) {
    super(textComponent, null, updateOn);
  }

  @Override
  protected String formatTextFromValue(final V value) {
    return getComponent().getDateTimeFormatter().format(value);
  }

  @Override
  protected V parseValueFromText(final String text) {
    try {
      return getComponent().getDateTimeParser().parse(text, getComponent().getDateTimeFormatter());
    }
    catch (final DateTimeParseException e) {
      return null;
    }
  }
}
