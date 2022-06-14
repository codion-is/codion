/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * Misc. utilities.
 */
public final class Util {

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

  /**
   * Maps the given values according to the keys provided by the given key provider,
   * keeping the iteration order of the given collection. Null keys are allowed.
   * <pre>
   * class Person {
   *   String name;
   *   Integer age;
   *
   *   public Integer getAge() {
   *     return age;
   *   }
   * }
   *
   * List&#60;Person&#62; persons = ...;
   *
   * Map&#60;Integer, List&#60;Person&#62;&#62; personsByAge = Util.map(persons, Person::getAge);
   * </pre>
   * @param values the values to map
   * @param keyProvider the object providing keys for values
   * @param <K> the key type
   * @param <T> the value type
   * @return a LinkedHashMap with the values mapped to their respective key values, respecting the iteration order of the given collection
   */
  public static <K, T> LinkedHashMap<K, List<T>> map(Collection<? extends T> values, Function<T, K> keyProvider) {
    requireNonNull(values, "values");
    requireNonNull(keyProvider, "keyProvider");
    LinkedHashMap<K, List<T>> map = new LinkedHashMap<>(values.size());
    for (T value : values) {
      map.computeIfAbsent(keyProvider.apply(value), k -> new ArrayList<>()).add(value);
    }

    return map;
  }

  /**
   * @param className the name of the class to search for
   * @return true if the given class is found on the classpath
   */
  public static boolean onClasspath(String className) {
    try {
      Class.forName(requireNonNull(className, "className"));
      return true;
    }
    catch (ClassNotFoundException e) {
      return false;
    }
  }

  /**
   * Rounds the given double to {@code places} decimal places, using {@link RoundingMode#HALF_UP}.
   * @param d the double to round, null results in a null return value
   * @param places the number of decimal places
   * @return the rounded value or null if the parameter value was null
   */
  public static Double roundDouble(Double d, int places) {
    return roundDouble(d, places, RoundingMode.HALF_UP);
  }

  /**
   * Rounds the given double to {@code places} decimal places.
   * @param d the double to round, null results in a null return value
   * @param places the number of decimal places
   * @param roundingMode the rounding mode
   * @return the rounded value or null if the parameter value was null
   */
  public static Double roundDouble(Double d, int places, RoundingMode roundingMode) {
    try {
      return d == null ? null : new BigDecimal(Double.toString(d)).setScale(places, roundingMode).doubleValue();
    }
    catch (NumberFormatException e) {
      throw new RuntimeException(e);
    }
  }
}
