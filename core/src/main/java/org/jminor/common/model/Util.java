/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.text.Collator;
import java.text.FieldPosition;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ThreadFactory;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * A static utility class.
 */
public final class Util {

  /**
   * The name of the file containing the current version information
   */
  public static final String VERSION_FILE = "version.txt";
  /**
   * The name of the preferences key used to save the default username
   */
  public static final String PREFERENCE_DEFAULT_USERNAME = "jminor.username";
  /**
   * The line separator for the current system
   */
  public static final String LINE_SEPARATOR = System.getProperty("line.separator");
  /**
   * The file separator for the current system
   */
  public static final String FILE_SEPARATOR = System.getProperty("file.separator");
  /**
   * The system property key for specifying a ssl truststore
   */
  public static final String JAVAX_NET_NET_TRUSTSTORE = "javax.net.ssl.trustStore";
  /**
   * A Format object performing no formatting
   */
  public static final Format NULL_FORMAT = new NullFormat();

  private static final Logger LOG = LoggerFactory.getLogger(Util.class);
  private static final Random RANDOM = new Random();
  private static final Version VERSION;
  private static final int K = 1024;
  private static final String SPACE = " ";
  private static final String UNDERSCORE = "_";
  private static final String KEY = "key";
  private static final int INPUT_BUFFER_SIZE = 8192;
  private static final int TEN = 10;
  private static Preferences userPreferences;

  static {
    try {
      VERSION = Version.parse(getTextFileContents(Util.class, VERSION_FILE));
    }
    catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Util() {}

  /**
   * Specifies the main configuration file.<br>
   * Value type: String<br>
   * Default value: null
   * @see #parseConfigurationFile()
   */
  public static final String CONFIGURATION_FILE = "jminor.configurationFile";

  /**
   * Add a property with this name in the main configuration file and specify a comma separated list
   * of additional configuration files that should be parsed along with the main configuration file.<br>
   * Value type: String<br>
   * Default value: null
   * @see #parseConfigurationFile()
   */
  public static final String ADDITIONAL_CONFIGURATION_FILES = "jminor.additionalConfigurationFiles";

  /**
   * Returns true if the given host is reachable, false if it is not or an exception is thrown while trying
   * @param host the hostname
   * @param timeout the timeout in milliseconds
   * @return true if the host is reachable
   */
  public static boolean isHostReachable(final String host, final int timeout) {
    rejectNullValue(host, "host");
    try {
      return InetAddress.getByName(host).isReachable(timeout);
    }
    catch (final IOException e) {
      return false;
    }
  }

  /**
   * @param key the key identifying the preference
   * @param defaultValue the default value if no preference is available
   * @return the user preference associated with the given key
   */
  public static String getUserPreference(final String key, final String defaultValue) {
    rejectNullValue(key, KEY);
    return getUserPreferences().get(key, defaultValue);
  }

  /**
   * @param key the key to use to identify the preference
   * @param value the preference value to associate with the given key
   */
  public static void putUserPreference(final String key, final String value) {
    rejectNullValue(key, KEY);
    getUserPreferences().put(key, value);
  }

  /**
   * Removes the preference associated with the given key
   * @param key the key to use to identify the preference to remove
   */
  public static void removeUserPreference(final String key) {
    rejectNullValue(key, KEY);
    getUserPreferences().remove(key);
  }

  /**
   * Flushes the preferences to disk
   * @throws BackingStoreException in case of a backing store failure
   */
  public static void flushUserPreferences() throws BackingStoreException {
    getUserPreferences().flush();
  }

  /**
   * Retrieves the default username for the given application identifier saved in preferences, if any
   * @param applicationIdentifier the application identifier
   * @param defaultName the name to use if none is found in the preferences
   * @return the default username
   */
  public static String getDefaultUserName(final String applicationIdentifier, final String defaultName) {
    rejectNullValue(applicationIdentifier, "applicationIdentifier");
    return getUserPreference(applicationIdentifier + "." + PREFERENCE_DEFAULT_USERNAME, defaultName);
  }

  /**
   * Saves the default username for the given application identifier
   * @param applicationIdentifier the application identifier
   * @param username the username
   */
  public static void setDefaultUserName(final String applicationIdentifier, final String username) {
    rejectNullValue(applicationIdentifier, "applicationIdentifier");
    putUserPreference(applicationIdentifier + "." + PREFERENCE_DEFAULT_USERNAME, username);
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
   * Parses an Integer from the given String.
   * A null or empty String results in null, "-" in -1.
   * @param text the text to parse
   * @return an Integer based on the given text
   */
  public static Integer getInt(final String text) {
    if (text == null) {
      return null;
    }

    final String noGrouping = text.replace(".", "").replace(",", "");
    if (noGrouping.length() == 0) {
      return null;
    }
    if (noGrouping.equals("-")) {
      return  -1;
    }

    return Integer.parseInt(noGrouping);
  }

  /**
   * Parses a Double from the given String.
   * A null or empty String results in null, "-" in -1.
   * @param text the text to parse
   * @return a Double based on the given text
   */
  public static Double getDouble(final String text) {
    if (nullOrEmpty(text)) {
      return null;
    }

    if (text.equals("-")) {
      return -1d;
    }

    return Double.parseDouble(text.replace(',', '.'));
  }

  /**
   * Parses a Long from the given String.
   * A null or empty String results in null, "-" in -1.
   * @param text the text to parse
   * @return a Long based on the given text
   */
  public static Long getLong(final String text) {
    if (text == null) {
      return null;
    }

    final String noGrouping = text.replace(".", "").replace(",", "");
    if (noGrouping.length() == 0) {
      return null;
    }
    if (noGrouping.equals("-")) {
      return  -1L;
    }

    return Long.parseLong(noGrouping);
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
   * @param filename the name of the file
   * @return the number of lines in the given file
   * @throws java.io.IOException in case the file can not be read
   */
  public static int countLines(final String filename) throws IOException {
    return countLines(new File(filename), null);
  }

  /**
   * @param file the file
   * @return the number of lines in the given file
   * @throws java.io.IOException in case the file can not be read
   */
  public static int countLines(final File file) throws IOException {
    return countLines(file, null);
  }

  /**
   * @param file the file
   * @param excludePrefix lines are excluded from the count if they start with this string
   * @return the number of lines in the given file
   * @throws java.io.IOException in case the file can not be read
   */
  public static int countLines(final File file, final String excludePrefix) throws IOException {
    try (final BufferedReader reader = new BufferedReader(new FileReader(file))) {
      int lines = 0;
      String line = reader.readLine();
      while (line != null) {
        if (excludePrefix == null || !line.startsWith(excludePrefix)) {
          lines++;
        }
        line = reader.readLine();
      }

      return lines;
    }
  }

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
   * @return a String containing all system properties, one per line
   */
  public static String getSystemProperties() {
    try {
      final SecurityManager manager = System.getSecurityManager();
      if (manager != null) {
        manager.checkPropertiesAccess();
      }
    }
    catch (final SecurityException e) {
      LOG.error(e.getMessage(), e);
      return "";
    }
    final Properties props = System.getProperties();
    final Enumeration propNames = props.propertyNames();
    final List<String> orderedPropertyNames = new ArrayList<>(props.size());
    while (propNames.hasMoreElements()) {
      orderedPropertyNames.add((String) propNames.nextElement());
    }

    Collections.sort(orderedPropertyNames);
    final StringBuilder propsString = new StringBuilder();
    for (final String key : orderedPropertyNames) {
      propsString.append(key).append(": ").append(props.getProperty(key)).append("\n");
    }

    return propsString.toString();
  }

  public static String getDelimitedString(final String[][] headers, final String[][] data, final String delimiter) {
    rejectNullValue(headers, "headers");
    rejectNullValue(data, "data");
    rejectNullValue(delimiter, "delimiter");
    final StringBuilder contents = new StringBuilder();
    for (final String[] header : headers) {
      for (int j = 0; j < header.length; j++) {
        contents.append(header[j]);
        if (j < header.length - 1) {
          contents.append(delimiter);
        }
      }
      contents.append(LINE_SEPARATOR);
    }

    for (final String[] someData : data) {
      for (int j = 0; j < someData.length; j++) {
        contents.append(someData[j]);
        if (j < someData.length - 1) {
          contents.append(delimiter);
        }
      }
      contents.append(LINE_SEPARATOR);
    }

    return contents.toString();
  }

  public static void writeDelimitedFile(final String[][] headers, final String[][] data, final String delimiter,
                                        final File file) throws IOException {
    writeFile(getDelimitedString(headers, data, delimiter), file);
  }

  /**
   * @param contents the contents to write to the file, overwriting the contents
   * @param file the file
   * @throws IOException in case of an exception
   */
  public static void writeFile(final String contents, final File file) throws IOException {
    writeFile(contents, file, false);
  }

  /**
   * @param contents the contents to write to the file
   * @param file the file
   * @param append if true the contents are appended, otherwise overwritten
   * @throws IOException in case of an exception
   */
  public static void writeFile(final String contents, final File file, final boolean append) throws IOException {
    rejectNullValue(contents, "contents");
    rejectNullValue(file, "file");
    try (final BufferedWriter writer = new BufferedWriter(new FileWriter(file, append))) {
      writer.write(contents);
    }
  }

  /**
   * Deserializes a list of Objects from the given file
   * @param file the file
   * @return deserialized objects
   * @throws SerializeException in case of an exception
   */
  public static List<Object> deserializeFromFile(final File file) throws SerializeException {
    final List<Object> objects = new ArrayList<>();
    try (final ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(file))) {
      while (true) {
        objects.add(inputStream.readObject());
      }
    }
    catch (final EOFException ignored) {/*ignored*/}
    catch (final Exception e) {
      throw new SerializeException(e.getMessage(), e);
    }

    return objects;
  }

  /**
   * Srializes a Collection of Objects to a given file
   * @param objects the objects to serialize
   * @param file the file
   * @throws SerializeException in case of an exception
   */
  public static void serializeToFile(final Collection objects, final File file) throws SerializeException {
    try (final ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file))) {
      for (final Object object : objects) {
        outputStream.writeObject(object);
      }
    }
    catch (final IOException e) {
      throw new SerializeException(e.getMessage(), e);
    }
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
   * @return a string containing the framework version number, without any version metadata (fx. build no.)
   */
  public static String getVersionString() {
    final String versionString = getVersionAndBuildNumberString();
    if (versionString.toLowerCase().contains("-")) {
      return versionString.substring(0, versionString.toLowerCase().indexOf("-"));
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
   * Rounds the given double to <code>places</code> decimal places
   * @param d the double to round
   * @param places the number of decimal places
   * @return the rounded value
   */
  public static double roundDouble(final double d, final int places) {
    return Math.round(d * Math.pow(TEN, (double) places)) / Math.pow(TEN, (double) places);
  }

  /**
   * @return a NumberFormat instance with grouping disabled
   */
  public static NumberFormat getNonGroupingNumberFormat() {
    return getNonGroupingNumberFormat(false);
  }

  /**
   * @param integerFormat if true an integer based number format is returned
   * @return a NumberFormat instance with grouping disabled
   */
  public static NumberFormat getNonGroupingNumberFormat(final boolean integerFormat) {
    final NumberFormat ret = integerFormat ? NumberFormat.getIntegerInstance() : NumberFormat.getNumberInstance();
    ret.setGroupingUsed(false);

    return ret;
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
   * @param file the file
   * @return the bytes comprising the given file
   * @throws IOException in case of an exception
   */
  public static byte[] getBytesFromFile(final File file) throws IOException {
    rejectNullValue(file, "file");
    try (final InputStream inputStream = new FileInputStream(file)) {
      // Get the size of the file
      final long length = file.length();

      // Create the byte array to hold the data
      final byte[] bytes = new byte[(int) length];

      // Read in the bytes
      int offset = 0;
      int numRead = inputStream.read(bytes, offset, bytes.length - offset);
      while (offset < bytes.length && numRead >= 0) {
        offset += numRead;
        numRead = inputStream.read(bytes, offset, bytes.length - offset);
      }

      // Ensure all the bytes have been read in
      if (offset < bytes.length) {
        throw new IOException("Could not completely read file " + file.getName());
      }

      return bytes;
    }
  }

  private static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

  /**
   * Creates a random string from alphanumeric uppercase characters
   * @param minLength the minimum length
   * @param maxLength the maximum length
   * @return a random string
   */
  public static String createRandomString(final int minLength, final int maxLength) {
    if (minLength > maxLength) {
      throw new IllegalArgumentException("Minimum length can not exceed maximum length");
    }
    final StringBuilder sb = new StringBuilder();
    final int length = minLength == maxLength ? minLength : RANDOM.nextInt(maxLength - minLength) + minLength;
    for( int i = 0; i < length; i++ ) {
      sb.append(AB.charAt(RANDOM.nextInt(AB.length())));
    }

    return sb.toString();
  }

  /**
   * Sorts the string representations of this lists contents, using
   * the space aware collator
   * @see #getSpaceAwareCollator()
   * @param values the list to sort (collate)
   */
  public static void collate(final List values) {
    Collections.sort(values, getSpaceAwareCollator());
  }

  /**
   * Creates a URI from the given URL or path
   * @param urlOrPath the URL or path
   * @return URI from the given URL or path
   * @throws URISyntaxException in case of an exception
   */
  public static URI getURI(final String urlOrPath) throws URISyntaxException {
    return getURIs(Collections.singletonList(urlOrPath)).iterator().next();
  }

  /**
   * Creates URIs from the given URLs or paths
   * @param urlsOrPaths the URLs or paths
   * @return URIs from the given URLs or paths
   * @throws URISyntaxException in case of an exception
   */
  public static Collection<URI> getURIs(final Collection<String> urlsOrPaths) throws URISyntaxException {
    final Collection<URI> urls = new ArrayList<>();
    for (final String urlsOrPath : urlsOrPaths) {
      if (urlsOrPath.toLowerCase().startsWith("http")) {
        urls.add(new URI(urlsOrPath.trim()));
      }
      else {
        urls.add(new File(urlsOrPath.trim()).toURI());
      }
    }

    return urls;
  }

  /**
   * @return a ThreadLocal version of the default Collator
   */
  public static ThreadLocal<Collator> getThreadLocalCollator() {
    return new ThreadLocal<Collator>() {
      @Override
      protected synchronized Collator initialValue() {
        return Collator.getInstance();
      }
    };
  }

  /**
   * Throws an IllegalArgumentException if the given string value is null or empty
   * @param value the string value
   * @param valueName the name of the value to include in the error message
   * @return the string value
   */
  public static String rejectNullOrEmpty(final String value, final String valueName) {
    if (value == null || value.isEmpty()) {
      throw new IllegalArgumentException(valueName + " is null or empty");
    }

    return value;
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
   * @param <T> the comparison type
   * @return a Comparator which compares the string representations of the objects
   * using the default Collator, taking spaces into account.
   */
  public static <T> Comparator<T> getSpaceAwareCollator() {
    return new Comparator<T>() {
      private final Collator collator = Collator.getInstance();
      @Override
      public int compare(final T o1, final T o2) {
        return collateSansSpaces(collator, o1.toString(), o2.toString());
      }
    };
  }

  /**
   * Collates the contents of the list, replacing spaces with underscores before sorting
   * @param collator the collator
   * @param list the list
   */
  public static void collateSansSpaces(final Collator collator, final List<?> list) {
    Collections.sort(list, new Comparator<Object>() {
      @Override
      public int compare(final Object o1, final Object o2) {
        return collateSansSpaces(collator, o1.toString(), o2.toString());
      }
    });
  }

  /**
   * Collates the given strings after replacing spaces with underscores
   * @param collator the collator to use
   * @param stringOne the first string
   * @param stringTwo the second string
   * @return the collation result
   */
  public static int collateSansSpaces(final Collator collator, final String stringOne, final String stringTwo) {
    rejectNullValue(collator, "collator");
    rejectNullValue(stringOne, "stringOne");
    rejectNullValue(stringTwo, "stringTwo");

    return collator.compare(stringOne.replaceAll(SPACE, UNDERSCORE), stringTwo.replaceAll(SPACE, UNDERSCORE));
  }

  /**
   * Closes the given Closeable instances, swallowing any Exceptions that occur
   * @param closeables the closeables to close
   */
  public static void closeSilently(final Closeable... closeables) {
    if (closeables == null) {
      return;
    }
    for (final Closeable closeable : closeables) {
      try {
        if (closeable != null) {
          closeable.close();
        }
      }
      catch (final Exception ignored) {/*ignored*/}
    }
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
   * Parses the configuration file specified by the {@link #CONFIGURATION_FILE} property,
   * adding the resulting properties via System.setProperty(key, value).
   * Also parses any configuration files specified by {@link #ADDITIONAL_CONFIGURATION_FILES}.
   * @see #CONFIGURATION_FILE
   */
  public static void parseConfigurationFile() {
    parseConfigurationFile(System.getProperty(CONFIGURATION_FILE));
  }

  /**
   * Parses the given configuration file adding the resulting properties via System.setProperty(key, value).
   * If a file with the given name is not found on the classpath we try to locate it on the filesystem,
   * relative to user.dir, if the file is not found a RuntimeException is thrown.
   * If the {@link #ADDITIONAL_CONFIGURATION_FILES} property is found, the files specified are parsed as well,
   * note that the actual property value is not added to the system properties.
   * @param filename the configuration filename
   * @throws IllegalArgumentException in case the configuration file is not found
   * @see #CONFIGURATION_FILE
   */
  public static void parseConfigurationFile(final String filename) {
    if (filename != null) {
      InputStream inputStream = null;
      String additionalConfigurationFiles = null;
      try {
        inputStream = ClassLoader.getSystemResourceAsStream(filename);
        if (inputStream == null) {//not on classpath
          final File configurationFile = new File(System.getProperty("user.dir") + File.separator + filename);
          if (!configurationFile.exists()) {
            throw new IllegalArgumentException("Configuration file not found on classpath (" + filename + ") or as a file (" + configurationFile.getPath() + ")");
          }
          inputStream = new FileInputStream(configurationFile);
          LOG.debug("Reading configuration file from filesystem: {}", filename);
        }
        else {
          LOG.debug("Reading configuration file from classpath: {}", filename);
        }
        final Properties properties = new Properties();
        properties.load(inputStream);
        for (final Map.Entry entry : properties.entrySet()) {
          final Object key = entry.getKey();
          final String value = (String) properties.get(key);
          LOG.debug("{} -> {}", key, value);
          if (key.equals(ADDITIONAL_CONFIGURATION_FILES)) {
            additionalConfigurationFiles = value;
          }
          else {
            System.setProperty((String) key, value);
          }
        }
      }
      catch (final IOException e) {
        throw new RuntimeException(e);
      }
      finally {
        closeSilently(inputStream);
      }
      if (additionalConfigurationFiles != null) {
        final String[] configurationFiles = additionalConfigurationFiles.split(",");
        for (final String configurationFile : configurationFiles) {
          parseConfigurationFile(configurationFile.trim());
        }
      }
    }
  }

  /**
   * Initializes a proxy instance for the given class, using the class loader of that class
   * @param clazz the class to proxy
   * @param invocationHandler the invocation handler to use
   * @param <T> the type
   * @return a proxy for the given class
   */
  @SuppressWarnings({"unchecked"})
  public static <T> T initializeProxy(final Class<T> clazz, final InvocationHandler invocationHandler) {
    return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] {clazz}, invocationHandler);
  }

  public static Exception unwrapAndLog(final Exception exception, final Class<? extends Exception> wrappingExceptionClass,
                                       final Logger logger) {
    return unwrapAndLog(exception, wrappingExceptionClass, logger, Collections.<Class<? extends Exception>>emptyList());
  }

  public static Exception unwrapAndLog(final Exception exception, final Class<? extends Exception> wrappingExceptionClass,
                                       final Logger logger, final Collection<Class<? extends Exception>> dontLog) {
    if (exception.getCause() instanceof Exception) {//else we can't really unwrap it
      if (wrappingExceptionClass.equals(exception.getClass())) {
        return unwrapAndLog((Exception) exception.getCause(), wrappingExceptionClass, logger);
      }

      if (dontLog != null && dontLog.contains(exception.getClass())) {
        return exception;
      }
    }
    if (logger != null) {
      logger.error(exception.getMessage(), exception);
    }

    return exception;
  }

  /**
   * @param valueType the class of the value for the given bean property
   * @param property the name of the bean property for which to retrieve the set method
   * @param valueOwner a bean instance
   * @return the method used to set the value of the linked property
   * @throws NoSuchMethodException if the method does not exist in the owner class
   */
  public static Method getSetMethod(final Class valueType, final String property, final Object valueOwner) throws NoSuchMethodException {
    rejectNullValue(valueOwner, "valueOwner");
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
    rejectNullValue(valueType, "valueType");
    rejectNullValue(property, "property");
    rejectNullValue(ownerClass, "ownerClass");
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
    rejectNullValue(valueOwner, "valueOwner");
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
    rejectNullValue(valueType, "valueType");
    rejectNullValue(property, "property");
    rejectNullValue(ownerClass, "ownerClass");
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

  /**
   * Reads the trust store specified by "javax.net.ssl.trustStore" from the classpath, copies it
   * to a temporary file and sets the trust store property so that it points to that temporary file.
   * If the trust store file specified is not found on the classpath this method has no effect.
   * @param temporaryFileNamePrefix the prefix to use for the temporary filename
   */
  public static void resolveTrustStoreFromClasspath(final String temporaryFileNamePrefix) {
    final String value = System.getProperty(JAVAX_NET_NET_TRUSTSTORE);
    if (nullOrEmpty(value)) {
      LOG.debug("No trust store specified via {}", JAVAX_NET_NET_TRUSTSTORE);
      return;
    }
    FileOutputStream out = null;
    InputStream in = null;
    try {
      final ClassLoader loader = Util.class.getClassLoader();
      in = loader.getResourceAsStream(value);
      if (in == null) {
        LOG.debug("Specified trust store not found on classpath: {}", value);
        return;
      }
      final File file = File.createTempFile(temporaryFileNamePrefix, "tmp");
      file.deleteOnExit();
      out = new FileOutputStream(file);
      final byte[] buf = new byte[INPUT_BUFFER_SIZE];
      int br = in.read(buf);
      while (br > 0) {
        out.write(buf, 0, br);
        br = in.read(buf);
      }
      LOG.debug("Classpath trust store resolved to file: {} -> {}", JAVAX_NET_NET_TRUSTSTORE, file);

      System.setProperty(JAVAX_NET_NET_TRUSTSTORE, file.getPath());
    }
    catch (final IOException e) {
      throw new RuntimeException(e);
    }
    finally {
      closeSilently(out, in);
    }
  }

  /**
   * Provides objects of type K, derived from a value of type V, for hashing said value via .hashCode().
   * @param <K> the type of the object to use for key generation via .hashCode()
   * @param <V> the value type
   * @see Util#map(java.util.Collection, MapKeyProvider)
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

  private static synchronized Preferences getUserPreferences() {
    if (userPreferences == null) {
      userPreferences = Preferences.userRoot();
    }

    return userPreferences;
  }

  /**
   * A ThreadFactory implementation producing daemon threads
   */
  public static final class DaemonThreadFactory implements ThreadFactory {
    @Override
    public Thread newThread(final Runnable runnable) {
      final Thread thread = new Thread(runnable);
      thread.setDaemon(true);

      return thread;
    }
  }

  /**
   * A simple null format, which performs no formatting
   */
  private static final class NullFormat extends Format {

    private static final long serialVersionUID = 1;

    @Override
    public StringBuffer format(final Object obj, final StringBuffer toAppendTo, final FieldPosition pos) {
      toAppendTo.append(obj.toString());
      return toAppendTo;
    }

    @Override
    public Object parseObject(final String source, final ParsePosition pos) {
      pos.setIndex(source.length());
      return source;
    }
  }
}