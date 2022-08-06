/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

/**
 * Parses a value from a string
 * @param <T> the value type
 */
public interface Parser<T> {

  /**
   * Parses a value from the given text
   * @param text the text to parse
   * @return a parse result
   */
  ParseResult<T> parse(String text);

  /**
   * The result of parsing a value from a String
   * @param <T> the resulting value type
   */
  interface ParseResult<T> {

    /**
     * @return the text being parsed
     */
    String text();

    /**
     * @return the parsed value
     */
    T value();

    /**
     * @return true if the parsing was successful
     */
    boolean successful();
  }
}
