/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

/**
 * Parses a value from a non-null string
 * @param <T> the value type
 */
public interface StringParser<T> {

  /**
   * Parses the given string value and returns a type T, it is assumed the string is not null
   * @param value the string to parse
   * @return a value of type T parsed from the given string
   */
  T parse(final String value);
}
