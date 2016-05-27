/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A base utility class with no external dependencies
 */
public class Util {

  /**
   * The line separator for the current system
   */
  public static final String LINE_SEPARATOR = System.getProperty("line.separator");
  private static final int K = 1024;
  protected static final String KEY = "key";

  protected Util() {}

  /**
   * Fetch the entire contents of a resource text file, and return it in a String, using the default Charset.
   * @param resourceClass the resource class
   * @param resourceName the name of the resource to retrieve
   * @return the contents of the resource file
   * @throws IOException in case an IOException occurs
   */
  public static String getTextFileContents(final Class resourceClass, final String resourceName) throws IOException {
    return getTextFileContents(resourceClass, resourceName, Charset.defaultCharset());
  }

  /**
   * Fetch the entire contents of a resource textfile, and return it in a String.
   * @param resourceClass the resource class
   * @param resourceName the name of the resource to retrieve
   * @param charset the Charset to use when reading the file contents
   * @return the contents of the resource file
   * @throws IOException in case an IOException occurs
   */
  public static String getTextFileContents(final Class resourceClass, final String resourceName, final Charset charset) throws IOException {
    Objects.requireNonNull(resourceClass, "resourceClass");
    Objects.requireNonNull(resourceName, "resourceName");
    final InputStream inputStream = resourceClass.getResourceAsStream(resourceName);
    if (inputStream == null) {
      throw new FileNotFoundException("Resource not found: '" + resourceName + "'");
    }

    return getTextFileContents(inputStream, charset);
  }

  /**
   * Fetch the entire contents of a textfile, and return it in a String
   * @param filename the name of the file
   * @param charset the charset to use
   * @return the file contents as a String
   * @throws IOException in case of an exception
   */
  public static String getTextFileContents(final String filename, final Charset charset) throws IOException {
    Objects.requireNonNull(filename, "filename");
    return getTextFileContents(new FileInputStream(new File(filename)), charset);
  }

  /**
   * Fetch the entire contents of an InputStream, and return it in a String
   * @param inputStream the input stream to read
   * @param charset the charset to use
   * @return the stream contents as a String
   * @throws IOException in case of an exception
   */
  public static String getTextFileContents(final InputStream inputStream, final Charset charset) throws IOException {
    Objects.requireNonNull(inputStream, "inputStream");
    final StringBuilder contents = new StringBuilder();
    try (final BufferedReader input = new BufferedReader(new InputStreamReader(inputStream, charset))) {
      String line = input.readLine();
      while (line != null) {
        contents.append(line);
        contents.append(LINE_SEPARATOR);
        line = input.readLine();
      }
    }

    return contents.toString();
  }

  /**
   * @param strings the strings to check
   * @return true if one of the given strings is null or empty or if no arguments are provided, false otherwise
   */
  public static boolean nullOrEmpty(final String... strings) {
    if (strings == null || strings.length == 0) {
      return true;
    }
    for (final String string : strings) {
      if (string == null || string.length() == 0) {
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
    for (final Object object : objects) {
      if (object == null) {
        return false;
      }
    }

    return true;
  }

  /**
   * Maps the given values according to the keys provided by the given key provider,
   * keeping the iteration order of the given collection.
   * <code>
   * class Person {
   *   String name;
   *   Integer age;
   *   ...
   * }
   *
   * List&#60;Person&#62; persons = ...;
   * MapKeyProvider ageKeyProvider = new MapKeyProvider&#60;Integer, Person&#62;() {
   *   public Integer getKey(Person person) {
   *     return person.getAge();
   *   }
   * };
   * Map&#60;Integer, Collection&#60;Person&#62;&#62; personsByAge = Util.map(persons, ageKeyProvider);
   * </code>
   * @param values the values to map
   * @param keyProvider the object providing keys for values
   * @param <K> the key type
   * @param <V> the value type
   * @return a LinkedHashMap with the values mapped to their respective key values, respecting the iteration order of the given collection
   */
  public static <K, V> LinkedHashMap<K, Collection<V>> map(final Collection<V> values, final MapKeyProvider<K, V> keyProvider) {
    Objects.requireNonNull(values, "values");
    Objects.requireNonNull(keyProvider, "keyProvider");
    final LinkedHashMap<K, Collection<V>> map = new LinkedHashMap<>(values.size());
    for (final V value : values) {
      map(map, value, keyProvider.getKey(value));
    }

    return map;
  }

  /**
   * @param className the name of the class to search for
   * @return true if the given class is found on the classpath
   */
  public static boolean onClasspath(final String className) {
    Objects.requireNonNull(className, "className");
    try {
      Class.forName(className);
      return true;
    }
    catch (final ClassNotFoundException e) {
      return false;
    }
  }

  /**
   * Throws a RuntimeException in case the given string value is null or an empty string,
   * using <code>propertyName</code> in the error message, as in: "propertyName is required"
   * @param propertyName the name of the property that is required
   * @param value the value
   * @throws RuntimeException in case the value is null
   */
  public static void require(final String propertyName, final String value) {
    if (nullOrEmpty(value)) {
      throw new RuntimeException(propertyName + " is required");
    }
  }

  /**
   * @param maps the maps to check
   * @return true if one of the given maps is null or empty or if no arguments are provided, false otherwise
   */
  public static boolean nullOrEmpty(final Map... maps) {
    if (maps == null) {
      return true;
    }
    for (final Map map : maps) {
      if (map == null || map.isEmpty()) {
        return true;
      }
    }

    return false;
  }

  /**
   * @param collections the collections to check
   * @return true if one of the given collections is null or empty or if no arguments are provided, false otherwise
   */
  public static boolean nullOrEmpty(final Collection... collections) {
    if (collections == null) {
      return true;
    }
    for (final Collection collection : collections) {
      if (collection == null || collection.isEmpty()) {
        return true;
      }
    }

    return false;
  }

  /**
   * @param collection the collection
   * @param onePerLine if true then each item is put on a separate line, otherwise a comma separator is used
   * @return the collection contents as a string (using toString())
   */
  public static String getCollectionContentsAsString(final Collection<?> collection, final boolean onePerLine) {
    if (collection == null) {
      return "";
    }

    return getArrayContentsAsString(collection.toArray(), onePerLine);
  }

  /**
   * @param items the items
   * @param onePerLine if true then each item is put on a separate line, otherwise a comma separator is used
   * @return the array contents as a string (using toString())
   */
  public static String getArrayContentsAsString(final Object[] items, final boolean onePerLine) {
    if (items == null) {
      return "";
    }

    final StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < items.length; i++) {
      final Object item = items[i];
      if (item instanceof Object[]) {
        stringBuilder.append(getArrayContentsAsString((Object[]) item, onePerLine));
      }
      else if (!onePerLine) {
        stringBuilder.append(item).append(i < items.length - 1 ? ", " : "");
      }
      else {
        stringBuilder.append(item).append("\n");
      }
    }

    return stringBuilder.toString();
  }

  /**
   * Pads the given string with the given pad character until a length of <code>length</code> has been reached
   * @param string the string to pad
   * @param length the desired length
   * @param padChar the character to use for padding
   * @param left if true then the padding is added on the strings left side, otherwise the right side
   * @return the padded string
   */
  public static String padString(final String string, final int length, final char padChar, final boolean left) {
    Objects.requireNonNull(string, "string");
    if (string.length() >= length) {
      return string;
    }

    final StringBuilder stringBuilder = new StringBuilder(string);
    while (stringBuilder.length() < length) {
      if (left) {
        stringBuilder.insert(0, padChar);
      }
      else {
        stringBuilder.append(padChar);
      }
    }

    return stringBuilder.toString();
  }

  /**
   * Provides objects of type K, derived from a value of type V, for hashing said value via .hashCode().
   * @param <K> the type of the object to use for key generation via .hashCode()
   * @param <V> the value type
   * @see org.jminor.common.model.Util#map(java.util.Collection, MapKeyProvider)
   */
  public interface MapKeyProvider<K, V> {
    K getKey(final V value);
  }

  /**
   * @return the total memory allocated by this JVM in kilobytes
   */
  public static long getAllocatedMemory() {
    return Runtime.getRuntime().totalMemory() / K;
  }

  /**
   * @return the free memory available to this JVM in kilobytes
   */
  public static long getFreeMemory() {
    return Runtime.getRuntime().freeMemory() / K;
  }

  /**
   * @return the maximum memory available to this JVM in kilobytes
   */
  public static long getMaxMemory() {
    return Runtime.getRuntime().maxMemory() / K;
  }

  /**
   * @return the memory used by this JVM in kilobytes
   */
  public static long getUsedMemory() {
    return getAllocatedMemory() - getFreeMemory();
  }

  /**
   * @return a String indicating the memory usage of this JVM
   */
  public static String getMemoryUsageString() {
    return getUsedMemory() + " KB";
  }

  /**
   * @param valueType the class of the value for the given bean property
   * @param property the name of the bean property for which to retrieve the set method
   * @param valueOwner a bean instance
   * @return the method used to set the value of the linked property
   * @throws NoSuchMethodException if the method does not exist in the owner class
   */
  public static Method getSetMethod(final Class valueType, final String property, final Object valueOwner) throws NoSuchMethodException {
    Objects.requireNonNull(valueOwner, "valueOwner");
    return getSetMethod(valueType, property, valueOwner.getClass());
  }

  /**
   * @param valueType the class of the value for the given bean property
   * @param property the name of the bean property for which to retrieve the set method
   * @param ownerClass the bean class
   * @return the method used to set the value of the linked property
   * @throws NoSuchMethodException if the method does not exist in the owner class
   */
  public static Method getSetMethod(final Class valueType, final String property, final Class<?> ownerClass) throws NoSuchMethodException {
    Objects.requireNonNull(valueType, "valueType");
    Objects.requireNonNull(property, "property");
    Objects.requireNonNull(ownerClass, "ownerClass");
    if (property.length() == 0) {
      throw new IllegalArgumentException("Property must be specified");
    }
    final String propertyName = Character.toUpperCase(property.charAt(0)) + property.substring(1);
    return ownerClass.getMethod("set" + propertyName, valueType);
  }

  /**
   * @param valueType the class of the value for the given bean property
   * @param property the name of the bean property for which to retrieve the get method
   * @param valueOwner a bean instance
   * @return the method used to get the value of the linked property
   * @throws NoSuchMethodException if the method does not exist in the owner class
   */
  public static Method getGetMethod(final Class valueType, final String property, final Object valueOwner) throws NoSuchMethodException {
    Objects.requireNonNull(valueOwner, "valueOwner");
    return getGetMethod(valueType, property, valueOwner.getClass());
  }

  /**
   * @param valueType the class of the value for the given bean property
   * @param property the name of the bean property for which to retrieve the get method
   * @param ownerClass the bean class
   * @return the method used to get the value of the linked property
   * @throws NoSuchMethodException if the method does not exist in the owner class
   */
  public static Method getGetMethod(final Class valueType, final String property, final Class<?> ownerClass) throws NoSuchMethodException {
    Objects.requireNonNull(valueType, "valueType");
    Objects.requireNonNull(property, "property");
    Objects.requireNonNull(ownerClass, "ownerClass");
    if (property.length() == 0) {
      throw new IllegalArgumentException("Property must be specified");
    }
    final String propertyName = Character.toUpperCase(property.charAt(0)) + property.substring(1);
    if (valueType.equals(boolean.class) || valueType.equals(Boolean.class)) {
      try {
        return ownerClass.getMethod("is" + propertyName);
      }
      catch (final NoSuchMethodException ignored) {/*ignored*/}
      try {
        return ownerClass.getMethod(propertyName.substring(0, 1).toLowerCase()
                + propertyName.substring(1, propertyName.length()));
      }
      catch (final NoSuchMethodException ignored) {/*ignored*/}
    }

    return ownerClass.getMethod("get" + propertyName);
  }

  private static <K, V> void map(final Map<K, Collection<V>> map, final V value, final K key) {
    Objects.requireNonNull(value, "value");
    Objects.requireNonNull(key, KEY);
    Objects.requireNonNull(map, "map");
    if (!map.containsKey(key)) {
      map.put(key, new ArrayList<>());
    }

    map.get(key).add(value);
  }
}
