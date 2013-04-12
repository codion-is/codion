/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
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
import java.util.Arrays;
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

  public static final String VERSION_FILE = "version.txt";

  public static final String PREF_DEFAULT_USERNAME = "jminor.username";
  public static final String LINE_SEPARATOR = System.getProperty("line.separator");
  public static final String JAVAX_NET_NET_TRUSTSTORE = "javax.net.ssl.trustStore";

  private static final Logger LOG = LoggerFactory.getLogger(Util.class);
  private static final Random RANDOM = new Random();
  private static final int K = 1024;
  private static final String SPACE = " ";
  private static final String UNDERSCORE = "_";
  private static final int INPUT_BUFFER_SIZE = 8192;
  private static Preferences userPreferences;

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
    catch (IOException e) {
      return false;
    }
  }

  public static String getUserPreference(final String key, final String defaultValue) {
    rejectNullValue(key, "key");
    return getUserPreferences().get(key, defaultValue);
  }

  public static void putUserPreference(final String key, final String value) {
    rejectNullValue(key, "key");
    getUserPreferences().put(key, value);
  }

  public static void removeUserPreference(final String key) {
    rejectNullValue(key, "key");
    getUserPreferences().remove(key);
  }

  public static void flushUserPreferences() throws BackingStoreException {
    getUserPreferences().flush();
  }

  public static String getDefaultUserName(final String applicationIdentifier, final String defaultName) {
    rejectNullValue(applicationIdentifier, "applicationIdentifier");
    return getUserPreference(applicationIdentifier + "." + PREF_DEFAULT_USERNAME, defaultName);
  }

  public static void setDefaultUserName(final String applicationClassName, final String username) {
    rejectNullValue(applicationClassName, "applicationClassName");
    putUserPreference(applicationClassName + "." + PREF_DEFAULT_USERNAME, username);
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

    final String noGrouping = text.replace(".", "");
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

    final String noGrouping = text.replace(".", "");
    if (noGrouping.length() == 0) {
      return null;
    }
    if (noGrouping.equals("-")) {
      return  -1l;
    }

    return Long.parseLong(noGrouping);
  }

  public static String getCollectionContentsAsString(final Collection<?> collection, final boolean onePerLine) {
    if (collection == null) {
      return "";
    }

    return getArrayContentsAsString(collection.toArray(), onePerLine);
  }

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

  public static long getAllocatedMemory() {
    return Runtime.getRuntime().totalMemory() / K;
  }

  public static long getFreeMemory() {
    return Runtime.getRuntime().freeMemory() / K;
  }

  public static long getMaxMemory() {
    return Runtime.getRuntime().maxMemory() / K;
  }

  public static long getUsedMemory() {
    return getAllocatedMemory() - getFreeMemory();
  }

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
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader(file));
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
    finally {
      Util.closeSilently(reader);
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

  public static String getTextFileContents(final String filename, final Charset charset) throws IOException {
    rejectNullValue(filename, "filename");
    return getTextFileContents(new FileInputStream(new File(filename)), charset);
  }

  public static String getTextFileContents(final InputStream inputStream, final Charset charset) throws IOException {
    rejectNullValue(inputStream, "inputStream");
    final StringBuilder contents = new StringBuilder();
    BufferedReader input = null;
    try {
      input = new BufferedReader(new InputStreamReader(inputStream, charset));
      String line = input.readLine();
      while (line != null) {
        contents.append(line);
        contents.append(LINE_SEPARATOR);
        line = input.readLine();
      }
    }
    finally {
      closeSilently(input);
    }

    return contents.toString();
  }

  public static String getSystemProperties() {
    try {
      final SecurityManager manager = System.getSecurityManager();
      if (manager != null) {
        manager.checkPropertiesAccess();
      }
    }
    catch (SecurityException e) {
      return "";
    }
    final Properties props = System.getProperties();
    final Enumeration propNames = props.propertyNames();
    final List<String> orderedPropertyNames = new ArrayList<String>(props.size());
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

  public static void writeFile(final String contents, final File file) throws IOException {
    writeFile(contents, file, false);
  }

  public static void writeFile(final String contents, final File file, final boolean append) throws IOException {
    rejectNullValue(contents, "contents");
    rejectNullValue(file, "file");
    BufferedWriter writer = null;
    try {
      final FileWriter fileWriter = new FileWriter(file, append);
      writer = new BufferedWriter(fileWriter);
      writer.write(contents);
    }
    finally {
      closeSilently(writer);
    }
  }

  public static List<Object> deserializeFromFile(final File file) {
    final List<Object> objects = new ArrayList<Object>();
    ObjectInputStream inputStream = null;
    try {
      inputStream = new ObjectInputStream(new FileInputStream(file));
      while (true) {
        objects.add(inputStream.readObject());
      }
    }
    catch (EOFException ignored) {}
    catch (Exception e) {
      throw new RuntimeException(e);
    }
    finally {
      closeSilently(inputStream);
    }

    return objects;
  }

  public static void serializeToFile(final Collection objects, final File file) {
    ObjectOutputStream outputStream = null;
    try {
      outputStream = new ObjectOutputStream(new FileOutputStream(file));
      for (final Object object : objects) {
        outputStream.writeObject(object);
      }
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
    finally {
      closeSilently(outputStream);
    }
  }

  /**
   * True if the given objects are equal. Both objects being null results in true.
   * @param one the first object
   * @param two the second object
   * @return true if the given objects are equal
   */
  public static boolean equal(final Object one, final Object two) {
    return one == null && two == null || !(one == null ^ two == null) && one.equals(two);
  }

  public static String getVersion() {
    final String versionString = getVersionAndBuildNumber();
    if (versionString.toLowerCase().contains("build")) {
      return versionString.substring(0, versionString.toLowerCase().indexOf("build") - 1);
    }

    return "N/A";
  }

  public static String getVersionAndBuildNumber() {
    try {
      return getTextFileContents(Util.class, VERSION_FILE);
    }
    catch (IOException e) {
      return "N/A";
    }
  }

  /**
   * Rounds the given double to <code>places</code> decimal places
   * @param d the double to round
   * @param places the number of decimal places
   * @return the rounded value
   */
  public static double roundDouble(final double d, final int places) {
    return Math.round(d * Math.pow(10, (double) places)) / Math.pow(10, (double) places);
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

  public static byte[] getBytesFromFile(final File file) throws IOException {
    rejectNullValue(file, "file");
    InputStream inputStream = null;
    try {
      inputStream = new FileInputStream(file);

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
    finally {
      closeSilently(inputStream);
    }
  }

  private static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

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
   * Sorts the string representations of this list's contents, using
   * the space aware collator
   * @see #getSpaceAwareCollator()
   * @param values the list to sort (collate)
   */
  public static void collate(final List<?> values) {
    Collections.sort(values, getSpaceAwareCollator());
  }

  public static URI getURI(final String urlOrPath) throws URISyntaxException {
    return getURIs(Arrays.asList(urlOrPath)).iterator().next();
  }

  public static Collection<URI> getURIs(final Collection<String> urlsOrPaths) throws URISyntaxException {
    final Collection<URI> urls = new ArrayList<URI>();
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
   * Throws an IllegalArgumentException complaining about <code>valueName</code> being null
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
   * @return a Comparator which compares the string representations of the objects
   * using the default Collator, taking spaces into account.
   */
  public static <T> Comparator<T> getSpaceAwareCollator() {
    return new Comparator<T>() {
      private final Collator collator = Collator.getInstance();
      /** {@inheritDoc} */
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
      catch (Exception ignored) {}
    }
  }

  /**
   * Maps the given values according to the keys provided by the given key provider,
   * keeping the iteration order of the given collection.
   * <code>
   * <pre>
   * class Person {
   *   String name;
   *   Integer age;
   *   ...
   * }
   *
   * List<Person> persons = ...;
   * HashKeyProvider ageKeyProvider = new HashKeyProvider<Integer, Person>() {
   *   public Integer getKey(Person person) {
   *     return person.getAge();
   *   }
   * };
   * Map<Integer, Collection<Person>> personsByAge = Util.map(persons, ageKeyProvider);
   * </pre>
   * </code>
   * @param values the values to map
   * @param keyProvider the object providing keys to use when hashing the values
   * @param <K> the key type
   * @param <V> the value type
   * @return a map with the values hashed by their respective key values, respecting the iteration order of the given collection
   */
  public static <K, V> LinkedHashMap<K, Collection<V>> map(final Collection<V> values, final HashKeyProvider<K, V> keyProvider) {
    rejectNullValue(values, "values");
    rejectNullValue(keyProvider, "keyProvider");
    final LinkedHashMap<K, Collection<V>> map = new LinkedHashMap<K, Collection<V>>(values.size());
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
    catch (ClassNotFoundException e) {
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
   * @param maps the collections to check
   * @return true if one of the given collections is null or empty or if no arguments are provided, false otherwise
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
            throw new RuntimeException("Configuration file not found on classpath (" + filename + ") or as a file (" + configurationFile.getPath() + ")");
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
          LOG.debug("{} - > {}", key, value);
          if (key.equals(ADDITIONAL_CONFIGURATION_FILES)) {
            additionalConfigurationFiles = value;
          }
          else {
            System.setProperty((String) key, value);
          }
        }
      }
      catch (IOException e) {
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

  @SuppressWarnings({"unchecked"})
  public static <T> T initializeProxy(final Class<T> clazz, final InvocationHandler invocationHandler) {
    return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] {clazz}, invocationHandler);
  }

  public static Exception unwrapAndLog(final Exception exception, final Class<? extends Exception> wrappingExceptionClass,
                                       final Logger logger) {
    return unwrapAndLog(exception, wrappingExceptionClass, logger, (Class<? extends Exception>[]) null);
  }

  public static Exception unwrapAndLog(final Exception exception, final Class<? extends Exception> wrappingExceptionClass,
                                       final Logger logger, final Class<? extends Exception>... dontLog) {
    if (exception.getCause() instanceof Exception) {//else we can't really unwrap it
      if (wrappingExceptionClass.equals(exception.getClass())) {
        return unwrapAndLog((Exception) exception.getCause(), wrappingExceptionClass, logger);
      }

      if (dontLog != null) {
        for (final Class<? extends Exception> noLog : dontLog) {
          if (exception.getClass().equals(noLog)) {
            return exception;
          }
        }
      }
    }
    if (logger != null) {
      logger.error(exception.getMessage(), exception);
    }

    return exception;
  }

  /**
   * @param valueClass the class of the value for the given bean property
   * @param property the name of the bean property for which to retrieve the set method
   * @param valueOwner the bean object
   * @return the method used to set the value of the linked property
   * @throws NoSuchMethodException if the method does not exist in the owner class
   */
  public static Method getSetMethod(final Class valueClass, final String property, final Object valueOwner) throws NoSuchMethodException {
    Util.rejectNullValue(valueClass, "valueClass");
    Util.rejectNullValue(property, "property");
    Util.rejectNullValue(valueOwner, "valueOwner");
    if (property.length() == 0) {
      throw new IllegalArgumentException("Property must be specified");
    }
    final String propertyName = Character.toUpperCase(property.charAt(0)) + property.substring(1);
    return valueOwner.getClass().getMethod("set" + propertyName, valueClass);
  }

  /**
   * @param valueClass the class of the value for the given bean property
   * @param property the name of the bean property for which to retrieve the get method
   * @param valueOwner the bean object
   * @return the method used to get the value of the linked property
   * @throws NoSuchMethodException if the method does not exist in the owner class
   */
  public static Method getGetMethod(final Class valueClass, final String property, final Object valueOwner) throws NoSuchMethodException {
    Util.rejectNullValue(valueClass, "valueClass");
    Util.rejectNullValue(property, "property");
    Util.rejectNullValue(valueOwner, "valueOwner");
    if (property.length() == 0) {
      throw new IllegalArgumentException("Property must be specified");
    }
    final String propertyName = Character.toUpperCase(property.charAt(0)) + property.substring(1);
    if (valueClass.equals(boolean.class) || valueClass.equals(Boolean.class)) {
      try {
        return valueOwner.getClass().getMethod("is" + propertyName);
      }
      catch (NoSuchMethodException ignored) {}
      try {
        return valueOwner.getClass().getMethod(propertyName.substring(0, 1).toLowerCase()
                + propertyName.substring(1, propertyName.length()));
      }
      catch (NoSuchMethodException ignored) {}
    }

    return valueOwner.getClass().getMethod("get" + propertyName);
  }

  /**
   * Reads the trust store specified by "javax.net.ssl.trustStore" from the classpath, copies it
   * to a temporary file and sets the trust store property so that it points to that temporary file
   * @param temporaryFileNamePrefix the prefix to use for the temporary filename
   */
  public static void resolveTrustStore(final String temporaryFileNamePrefix) {
    final String value = System.getProperty(JAVAX_NET_NET_TRUSTSTORE);
    if (nullOrEmpty(value)) {
      LOG.debug("resolveTrustStoreProperty: {} is empty", JAVAX_NET_NET_TRUSTSTORE);
      return;
    }
    FileOutputStream out = null;
    InputStream in = null;
    try {
      final ClassLoader loader = Util.class.getClassLoader();
      in = loader.getResourceAsStream(value);
      if (in == null) {
        LOG.debug("resolveTrustStoreProperty: {} not found on classpath", value);
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
      LOG.debug("resolveTrustStoreProperty: {} -> {}", JAVAX_NET_NET_TRUSTSTORE, file);

      System.setProperty(JAVAX_NET_NET_TRUSTSTORE, file.getPath());
    }
    catch (IOException e) {
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
   * @see Util#map(java.util.Collection, org.jminor.common.model.Util.HashKeyProvider)
   */
  public interface HashKeyProvider<K, V> {
    K getKey(final V value);
  }

  private static <K, V> void map(final Map<K, Collection<V>> map, final V value, final K key) {
    rejectNullValue(value, "value");
    rejectNullValue(key, "key");
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
   /** {@inheritDoc} */
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
  public static final class NullFormat extends Format {

    private static final long serialVersionUID = 1;

    /** {@inheritDoc} */
    @Override
    public StringBuffer format(final Object obj, final StringBuffer toAppendTo, final FieldPosition pos) {
      toAppendTo.append(obj.toString());
      return toAppendTo;
    }

    /** {@inheritDoc} */
    @Override
    public Object parseObject(final String source, final ParsePosition pos) {
      pos.setIndex(source.length());
      return source;
    }
  }
}