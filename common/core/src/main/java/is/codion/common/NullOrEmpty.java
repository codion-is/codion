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
 * Copyright (c) 2016 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Utility methods for checking if arguments are null or empty, if applicable.
 */
public final class NullOrEmpty {

  private NullOrEmpty() {}

  /**
   * Returns true if the given string is null or empty.
   * @param string the string to check
   * @return true if the given string is null or empty, false otherwise
   */
  public static boolean nullOrEmpty(String string) {
    return string == null || string.isEmpty();
  }

  /**
   * Returns true if any of the given strings is null or empty.
   * @param strings the strings to check
   * @return true if one of the given strings is null or empty or if no arguments are provided, false otherwise
   */
  public static boolean nullOrEmpty(String... strings) {
    if (strings == null || strings.length == 0) {
      return true;
    }
    for (int i = 0; i < strings.length; i++) {
      if (nullOrEmpty(strings[i])) {
        return true;
      }
    }

    return false;
  }

  /**
   * Returns true if the given map is null or empty.
   * @param map the map to check
   * @return true if the given map is null or empty, false otherwise
   */
  public static boolean nullOrEmpty(Map<?, ?> map) {
    return map == null || map.isEmpty();
  }

  /**
   * Returns true if the given collection is null or empty.
   * @param collection the collection to check
   * @return true if the given collection is null or empty, false otherwise
   */
  public static boolean nullOrEmpty(Collection<?> collection) {
    return collection == null || collection.isEmpty();
  }

  /**
   * Checks if any of the given objects is null
   * @param objects the objects to check
   * @return true if none of the given objects is null
   */
  public static boolean notNull(Object... objects) {
    if (objects == null) {
      return false;
    }

    return Arrays.stream(objects).noneMatch(Objects::isNull);
  }
}
