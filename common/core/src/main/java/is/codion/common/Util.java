/*
 * Copyright (c) 2016 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Misc. utilities.
 */
public final class Util {

  private Util() {}

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
