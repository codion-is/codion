/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
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

  private static final Map<Class<?>, Class<?>> PRIMITIVE_BOXED_TYPE_MAP;
  private static final Map<Class<?>, Object> DEFAULT_PRIMITIVE_VALUES;

  private static boolean defaultBoolean;
  private static byte defaultByte;
  private static short defaultShort;
  private static int defaultInt;
  private static long defaultLong;
  private static float defaultFloat;
  private static double defaultDouble;

  static {
    PRIMITIVE_BOXED_TYPE_MAP = new HashMap<>();
    PRIMITIVE_BOXED_TYPE_MAP.put(Boolean.TYPE, Boolean.class);
    PRIMITIVE_BOXED_TYPE_MAP.put(Byte.TYPE, Byte.class);
    PRIMITIVE_BOXED_TYPE_MAP.put(Short.TYPE, Short.class);
    PRIMITIVE_BOXED_TYPE_MAP.put(Integer.TYPE, Integer.class);
    PRIMITIVE_BOXED_TYPE_MAP.put(Long.TYPE, Long.class);
    PRIMITIVE_BOXED_TYPE_MAP.put(Float.TYPE, Float.class);
    PRIMITIVE_BOXED_TYPE_MAP.put(Double.TYPE, Double.class);

    DEFAULT_PRIMITIVE_VALUES = new HashMap<>();
    DEFAULT_PRIMITIVE_VALUES.put(Boolean.TYPE, defaultBoolean);
    DEFAULT_PRIMITIVE_VALUES.put(Byte.TYPE, defaultByte);
    DEFAULT_PRIMITIVE_VALUES.put(Short.TYPE, defaultShort);
    DEFAULT_PRIMITIVE_VALUES.put(Integer.TYPE, defaultInt);
    DEFAULT_PRIMITIVE_VALUES.put(Long.TYPE, defaultLong);
    DEFAULT_PRIMITIVE_VALUES.put(Float.TYPE, defaultFloat);
    DEFAULT_PRIMITIVE_VALUES.put(Double.TYPE, defaultDouble);
  }

  private Util() {}

  /**
   * Returns true if the given string is null or empty.
   * @param string the string to check
   * @return true if the given string is null or empty, false otherwise
   */
  public static boolean nullOrEmpty(final String string) {
    return string == null || string.isEmpty();
  }

  /**
   * Returns true if any of the given strings is null or empty.
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
   * Returns true if the given map is null or empty.
   * @param map the map to check
   * @return true if the given map is null or empty, false otherwise
   */
  public static boolean nullOrEmpty(final Map<?, ?> map) {
    return map == null || map.isEmpty();
  }

  /**
   * Returns true if the given collection is null or empty.
   * @param collection the collection to check
   * @return true if the given collection is null or empty, false otherwise
   */
  public static boolean nullOrEmpty(final Collection<?> collection) {
    return collection == null || collection.isEmpty();
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
  public static <K, T> LinkedHashMap<K, List<T>> map(final Collection<? extends T> values, final Function<T, K> keyProvider) {
    requireNonNull(values, "values");
    requireNonNull(keyProvider, "keyProvider");
    final LinkedHashMap<K, List<T>> map = new LinkedHashMap<>(values.size());
    for (final T value : values) {
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
   * Rounds the given double to {@code places} decimal places, using {@link RoundingMode#HALF_UP}.
   * @param d the double to round, null results in a null return value
   * @param places the number of decimal places
   * @return the rounded value or null if the parameter value was null
   */
  public static Double roundDouble(final Double d, final int places) {
    return roundDouble(d, places, RoundingMode.HALF_UP);
  }

  /**
   * Rounds the given double to {@code places} decimal places.
   * @param d the double to round, null results in a null return value
   * @param places the number of decimal places
   * @param roundingMode the rounding mode
   * @return the rounded value or null if the parameter value was null
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
   * @param primitiveType the primitive type
   * @return the default value for the given typ
   * @throws IllegalArgumentException in case primitiveType is not a primitive
   */
  public static <T> T getPrimitiveDefaultValue(final Class<T> primitiveType) {
    final T defaultValue = (T) DEFAULT_PRIMITIVE_VALUES.get(requireNonNull(primitiveType));
    if (defaultValue == null) {
      throw new IllegalArgumentException("Not a primitive type: " + primitiveType);
    }

    return defaultValue;
  }

  /**
   * @param primitiveType the primitive type
   * @return the boxed type
   * @throws IllegalArgumentException in case primitiveType is not a primitive
   */
  public static Class<?> getPrimitiveBoxedType(final Class<?> primitiveType) {
    if (!requireNonNull(primitiveType).isPrimitive()) {
      throw new IllegalArgumentException("Not a primitive type: " + primitiveType);
    }

    return PRIMITIVE_BOXED_TYPE_MAP.get(requireNonNull(primitiveType));
  }
}
