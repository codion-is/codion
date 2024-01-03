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
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common;

/**
 * Utility class for separators.
 */
public final class Separators {

  /**
   * The line separator for the current system, specified by the 'line.separator' system property
   */
  public static final String LINE_SEPARATOR = System.getProperty("line.separator");

  /**
   * The file separator for the current system, specified by the 'file.separator' system property
   */
  public static final String FILE_SEPARATOR = System.getProperty("file.separator");

  /**
   * The path separator for the current system, specified by the 'path.separator' system property
   */
  public static final String PATH_SEPARATOR = System.getProperty("path.separator");

  private Separators() {}
}
