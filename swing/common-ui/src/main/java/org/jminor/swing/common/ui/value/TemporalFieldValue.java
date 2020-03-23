/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.common.DateParser;

import javax.swing.JFormattedTextField;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;

final class TemporalFieldValue<V extends Temporal> extends FormattedTextComponentValue<V, JFormattedTextField> {

  private final DateTimeFormatter formatter;
  private final DateParser<V> dateParser;

  TemporalFieldValue(final JFormattedTextField textComponent, final String dateFormat,
                     final UpdateOn updateOn, final DateParser<V> dateParser) {
    super(textComponent, null, updateOn);
    this.formatter = DateTimeFormatter.ofPattern(dateFormat);
    this.dateParser = dateParser;
  }

  @Override
  protected String formatTextFromValue(final V value) {
    return formatter.format(value);
  }

  @Override
  protected V parseValueFromText(final String text) {
    try {
      return dateParser.parse(text, formatter);
    }
    catch (final DateTimeParseException e) {
      return null;
    }
  }
}
