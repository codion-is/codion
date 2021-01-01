/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.common.DateTimeParser;

import javax.swing.JFormattedTextField;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;

final class TemporalFieldValue<V extends Temporal> extends FormattedTextComponentValue<V, JFormattedTextField> {

  private final DateTimeFormatter formatter;
  private final DateTimeParser<V> dateTimeParser;

  TemporalFieldValue(final JFormattedTextField textComponent, final String dateFormat,
                     final UpdateOn updateOn, final DateTimeParser<V> dateTimeParser) {
    super(textComponent, null, updateOn);
    this.formatter = DateTimeFormatter.ofPattern(dateFormat);
    this.dateTimeParser = dateTimeParser;
  }

  @Override
  protected String formatTextFromValue(final V value) {
    return formatter.format(value);
  }

  @Override
  protected V parseValueFromText(final String text) {
    try {
      return dateTimeParser.parse(text, formatter);
    }
    catch (final DateTimeParseException e) {
      return null;
    }
  }
}
