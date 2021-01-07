/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Misc. utilities.
 */
public final class Util {

  /**
   * The line separator for the current system
   */
  public static final String LINE_SEPARATOR = System.getProperty("line.separator");

  /**
   * The file separator for the current system
   */
  public static final String FILE_SEPARATOR = System.getProperty("file.separator");

  /**
   * The path separator for the current system
   */
  public static final String PATH_SEPARATOR = System.getProperty("path.separator");

  private Util() {}

  /**
   * @param string the string to check
   * @return true if the given string is null or empty, false otherwise
   */
  public static boolean nullOrEmpty(final String string) {
    return string == null || string.isEmpty();
  }

  /**
   * @param strings the strings to check
   * @return true if one of the given strings is null or empty or if no arguments are provided, false otherwise
   */
  public static boolean nullOrEmpty(final String... strings) {
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
   * Checks if any of the given objects is null
   * @param objects the objects to check
   * @return true if none of the given objects is null
   */
  public static boolean notNull(final Object... objects) {
    if (objects == null) {
      return false;
    }

    return Arrays.stream(objects).noneMatch(Objects::isNull);
  }

  /**
   * Maps the given values according to the keys provided by the given key provider,
   * keeping the iteration order of the given collection.
   * {@code
   * class Person {
   *   String name;
   *   Integer age;
   *   ...
   * }
   *
   * List&#60;Person&#62; persons = ...;
   * Function<Person, Integer> ageKeyProvider = new Function&#60;Person, Integer&#62;() {
   *   public Integer apply(Person person) {
   *     return person.getAge();
   *   }
   * };
   * Map&#60;Integer, List&#60;Person&#62;&#62; personsByAge = Util.map(persons, ageKeyProvider);
   * }
   * @param values the values to map
   * @param keyProvider the object providing keys for values
   * @param <K> the key type
   * @param <V> the value type
   * @return a LinkedHashMap with the values mapped to their respective key values, respecting the iteration order of the given collection
   */
  public static <K, V> LinkedHashMap<K, List<V>> map(final Collection<? extends V> values, final Function<V, K> keyProvider) {
    requireNonNull(values, "values");
    requireNonNull(keyProvider, "keyProvider");
    final LinkedHashMap<K, List<V>> map = new LinkedHashMap<>(values.size());
    for (final V value : values) {
      map.computeIfAbsent(keyProvider.apply(value), k -> new ArrayList<>()).add(value);
    }

    return map;
  }

  /**
   * @param className the name of the class to search for
   * @return true if the given class is found on the classpath
   */
  public static boolean onClasspath(final String className) {
    try {
      Class.forName(requireNonNull(className, "className"));
      return true;
    }
    catch (final ClassNotFoundException e) {
      return false;
    }
  }

  /**
   * @param maps the maps to check
   * @return true if one of the given maps is null or empty or if no arguments are provided, false otherwise
   */
  public static boolean nullOrEmpty(final Map<?, ?>... maps) {
    if (maps == null) {
      return true;
    }

    return Arrays.stream(maps).anyMatch(Util::nullOrEmpty);
  }

  /**
   * @param map the map to check
   * @return true if the given map is null or empty, false otherwise
   */
  public static boolean nullOrEmpty(final Map<?, ?> map) {
    return map == null || map.isEmpty();
  }

  /**
   * @param collections the collections to check
   * @return true if one of the given collections is null or empty or if no arguments are provided, false otherwise
   */
  public static boolean nullOrEmpty(final Collection<?>... collections) {
    if (collections == null) {
      return true;
    }

    return Arrays.stream(collections).anyMatch(Util::nullOrEmpty);
  }

  /**
   * @param collection the collection to check
   * @return true if the given collection is null or empty, false otherwise
   */
  public static boolean nullOrEmpty(final Collection<?> collection) {
    return collection == null || collection.isEmpty();
  }

  /**
   * Rounds the given double to {@code places} decimal places, using {@link RoundingMode.HALF_UP}.
   * @param d the double to round, null result in a null return value
   * @param places the number of decimal places
   * @return the rounded value
   */
  public static Double roundDouble(final Double d, final int places) {
    return roundDouble(d, places, RoundingMode.HALF_UP);
  }

  /**
   * Rounds the given double to {@code places} decimal places.
   * @param d the double to round, null result in a null return value
   * @param places the number of decimal places
   * @return the rounded value
   */
  public static Double roundDouble(final Double d, final int places, final RoundingMode roundingMode) {
    try {
      return d == null ? null : new BigDecimal(Double.toString(d)).setScale(places, roundingMode).doubleValue();
    }
    catch (final NumberFormatException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return a String containing all system properties, one per line
   */
  public static String getSystemProperties() {
    return getSystemProperties((property, value) -> value);
  }

  /**
   * Returns a String containing all system properties, written by the given {@link PropertyWriter}.
   * @param propertyWriter for specific property formatting or exclusions
   * @return a String containing all system properties, one per line
   */
  public static String getSystemProperties(final PropertyWriter propertyWriter) {
    requireNonNull(propertyWriter, "propertyWriter");
    try {
      final SecurityManager manager = System.getSecurityManager();
      if (manager != null) {
        manager.checkPropertiesAccess();
      }
    }
    catch (final SecurityException e) {
      System.err.println(e.getMessage());
      return "";
    }
    final Properties props = System.getProperties();
    final Enumeration<?> propNames = props.propertyNames();
    final List<String> propertyNames = new ArrayList<>(props.size());
    while (propNames.hasMoreElements()) {
      propertyNames.add((String) propNames.nextElement());
    }

    Collections.sort(propertyNames);

    return propertyNames.stream().map(key -> key + ": " +
            propertyWriter.writeValue(key, props.getProperty(key))).collect(Collectors.joining("\n"));
  }

  /**
   * Writes a property value
   */
  public interface PropertyWriter {

    /**
     * Writes the given value.
     * @param property the property
     * @param value the value
     * @return the value
     */
    String writeValue(String property, String value);
  }
}
