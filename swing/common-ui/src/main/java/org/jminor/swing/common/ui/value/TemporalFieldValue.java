/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.common.DateParser;

import javax.swing.JFormattedTextField;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;

import static org.jminor.common.Util.nullOrEmpty;

final class TemporalFieldValue<V extends Temporal> extends TextComponentValue<V, JFormattedTextField> {

  private final DateTimeFormatter formatter;
  private final DateParser<V> dateParser;

  TemporalFieldValue(final JFormattedTextField textComponent, final String dateFormat,
                     final boolean updateOnKeystroke, final DateParser<V> dateParser) {
    super(textComponent, null, updateOnKeystroke);
    this.formatter = DateTimeFormatter.ofPattern(dateFormat);
    this.dateParser = dateParser;
  }

  @Override
  protected String textFromValue(final V value) {
    if (value == null) {
      return null;
    }

    return formatter.format(value);
  }

  @Override
  protected V valueFromText(final String text) {
    if (nullOrEmpty(text)) {
      return null;
    }

    try {
      return dateParser.parse(text, formatter);
    }
    catch (final DateTimeParseException e) {
      return null;
    }
  }
}
