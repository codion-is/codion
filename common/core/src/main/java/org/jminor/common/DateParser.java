/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;

/**
 * Parses a Temporal value from text with a provided formatter
 * @param <T> the Temporal type
 */
public interface DateParser<T extends Temporal> {

  /**
   * Parses the given text with the given formatter
   * @param text the text to parse
   * @param formatter the formatter to use
   * @return the Temporal value
   * @throws DateTimeParseException if unable to parse the text
   */
  T parse(CharSequence text, DateTimeFormatter formatter);
}
