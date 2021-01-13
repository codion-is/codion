/*
 * Copyright (c) 2021 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

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
    String getText();

    /**
     * @return the parsed value
     */
    T getValue();

    /**
     * @return the number of characters added
     */
    int getCharetOffset();

    /**
     * @return true if the parsing was successful
     */
    boolean successful();
  }

  /**
   * Instantiates a new {@link ParseResult} instance.
   * @param <T> the value type
   * @param text the text being parsed
   * @param value the parsed value
   * @return a new parse result
   */
  static <T> ParseResult<T> parseResult(final String text, final T value) {
    return parseResult(text, value, 0);
  }

  /**
   * Instantiates a new {@link ParseResult} instance.
   * @param <T> the value type
   * @param text the text being parsed
   * @param value the parsed value
   * @param charetOffset the number of chars to move the cursor
   * @return a new parse result
   */
  static <T> ParseResult<T> parseResult(final String text, final T value, final int charetOffset) {
    return parseResult(text, value, charetOffset, true);
  }

  /**
   * Instantiates a new {@link ParseResult} instance.
   * @param <T> the value type
   * @param text the text being parsed
   * @param value the parsed value
   * @param charetOffset the number of chars to move the cursor
   * @param successful true if the parsing was successful
   * @return a new parse result
   */
  static <T> ParseResult<T> parseResult(final String text, final T value, final int charetOffset,
                                        final boolean successful) {
    return new DefaultParseResult<>(text, value, charetOffset, successful);
  }
}
