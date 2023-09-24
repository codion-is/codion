/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson.
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
