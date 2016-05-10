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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A base utility class with no external dependencies
 */
public class Util {

  /**
   * The name of the file containing the current version information
   */
  public static final String VERSION_FILE = "version.txt";
  /**
   * The line separator for the current system
   */
  public static final String LINE_SEPARATOR = System.getProperty("line.separator");
  private static final Version VERSION;
  protected static final String KEY = "key";

  static {
    try {
      VERSION = Version.parse(getTextFileContents(Util.class, VERSION_FILE));
    }
    catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

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
    rejectNullValue(resourceClass, "resourceClass");
    rejectNullValue(resourceName, "resourceName");
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
    rejectNullValue(filename, "filename");
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
    rejectNullValue(inputStream, "inputStream");
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
   * @return a string containing the framework version number, without any version metadata (fx. build no.)
   */
  public static String getVersionString() {
    final String versionString = getVersionAndBuildNumberString();
    if (versionString.toLowerCase().contains("-")) {
      return versionString.substring(0, versionString.toLowerCase().indexOf('-'));
    }

    return versionString;
  }

  /**
   * @return a string containing the framework version and version metadata
   */
  public static String getVersionAndBuildNumberString() {
    return VERSION.toString();
  }

  /**
   * @return the framework Version
   */
  public static Version getVersion() {
    return VERSION;
  }

  /**
   * Throws an IllegalArgumentException complaining about <code>valueName</code> being null
   * @param <T> type value type
   * @param value the value to check
   * @param valueName the name of the value being checked
   * @throws IllegalArgumentException if value is null
   * @return the value in case it was not null
   */
  public static <T> T rejectNullValue(final T value, final String valueName) {
    if (value == null) {
      throw new IllegalArgumentException(valueName + " is null");
    }

    return value;
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
   * True if the given objects are equal or if both are null
   * @param one the first object
   * @param two the second object
   * @return true if the given objects are equal or if both are null
   */
  public static boolean equal(final Object one, final Object two) {
    return one == null && two == null || !(one == null ^ two == null) && one.equals(two);
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
    rejectNullValue(values, "values");
    rejectNullValue(keyProvider, "keyProvider");
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
    rejectNullValue(className, "className");
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
    rejectNullValue(string, "string");
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

  private static <K, V> void map(final Map<K, Collection<V>> map, final V value, final K key) {
    rejectNullValue(value, "value");
    rejectNullValue(key, KEY);
    rejectNullValue(map, "map");
    if (!map.containsKey(key)) {
      map.put(key, new ArrayList<V>());
    }

    map.get(key).add(value);
  }
}
