/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.Collator;
import java.util.*;
import java.util.prefs.Preferences;

/**
 * A static utility class.
 */
public final class Util {

  private Util() {}

  public static final String VERSION_FILE = "version.txt";

  public static final String LOGGING_LEVEL_PROPERTY = "jminor.logging.level";
  public static final String LOGGING_LEVEL_INFO = "info";
  public static final String LOGGING_LEVEL_DEBUG = "debug";
  public static final String LOGGING_LEVEL_WARN = "warn";
  public static final String LOGGING_LEVEL_FATAL = "fatal";
  public static final String LOGGING_LEVEL_TRACE = "trace";

  public static final String PREF_DEFAULT_USERNAME = "jminor.username";

  private static final Random RANDOM = new Random();

  private static List<Logger> loggers = new ArrayList<Logger>();
  private static Level defaultLoggingLevel;
  private static Preferences userPreferences;

  static {
    final String loggingLevel = System.getProperty(LOGGING_LEVEL_PROPERTY, LOGGING_LEVEL_INFO);
    if (loggingLevel.equals(LOGGING_LEVEL_INFO)) {
      defaultLoggingLevel = Level.INFO;
    }
    else if (loggingLevel.equals(LOGGING_LEVEL_DEBUG)) {
      defaultLoggingLevel = Level.DEBUG;
    }
    else if (loggingLevel.equals(LOGGING_LEVEL_WARN)) {
      defaultLoggingLevel = Level.WARN;
    }
    else if (loggingLevel.equals(LOGGING_LEVEL_FATAL)) {
      defaultLoggingLevel = Level.FATAL;
    }
    else if (loggingLevel.equals(LOGGING_LEVEL_TRACE)) {
      defaultLoggingLevel = Level.TRACE;
    }
  }

  /**
   * Returns true if the given host is reachable, false if it is not or an exception is thrown while trying
   * @param host the hostname
   * @param timeout the timeout in milliseconds
   * @return true if the host is reachable
   */
  public static boolean isHostReachable(final String host, final int timeout) {
    rejectNullValue(host);
    try {
      return InetAddress.getByName(host).isReachable(timeout);
    }
    catch (IOException e) {
      return false;
    }
  }

  public static String getUserPreference(final String key, final String defaultValue) {
    rejectNullValue(key);
    if (userPreferences == null) {
      userPreferences = Preferences.userRoot();
    }
    return userPreferences.get(key, defaultValue);
  }

  public static void putUserPreference(final String key, final String value) {
    rejectNullValue(key);
    if (userPreferences == null) {
      userPreferences = Preferences.userRoot();
    }
    userPreferences.put(key, value);
  }

  public static String getDefaultUserName(final String applicationIdentifier, final String defaultName) {
    rejectNullValue(applicationIdentifier);
    return getUserPreference(applicationIdentifier + "." + PREF_DEFAULT_USERNAME, defaultName);
  }

  public static void setDefaultUserName(final String applicationClassName, final String username) {
    rejectNullValue(applicationClassName);
    putUserPreference(applicationClassName + "." + PREF_DEFAULT_USERNAME, username);
  }

  public static String padString(final String orig, final int length, final char padChar, final boolean left) {
    rejectNullValue(orig);
    if (orig.length() >= length) {
      return orig;
    }

    final StringBuilder stringBuilder = new StringBuilder(orig);
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
   * @return the current logging level
   */
  public static Level getLoggingLevel() {
    if (loggers.size() == 0) {
      return defaultLoggingLevel;
    }

    return loggers.get(0).getLevel();
  }

  public static void setDefaultLoggingLevel(final Level defaultLoggingLevel) {
    rejectNullValue(defaultLoggingLevel);
    Util.defaultLoggingLevel = defaultLoggingLevel;
  }

  public static void setLoggingLevel(final Level level) {
    rejectNullValue(level);
    for (final Logger logger : loggers) {
      logger.setLevel(level);
    }
  }

  public static Logger getLogger(final Class classToLog) {
    rejectNullValue(classToLog);
    final Logger logger = Logger.getLogger(classToLog);
    logger.setLevel(getLoggingLevel());
    loggers.add(logger);

    return logger;
  }

  public static Logger getLogger(final String name) {
    rejectNullValue(name);
    final Logger logger = Logger.getLogger(name);
    logger.setLevel(getLoggingLevel());
    loggers.add(logger);

    return logger;
  }

  public static Integer getInt(final String text) {
    if (text == null || text.length() == 0) {
      return null;
    }

    final String noGrouping = text.replace(".", "");

    int value;
    if ((noGrouping.length() > 0) && (!noGrouping.equals("-"))) {
      value = Integer.parseInt(noGrouping);
    }
    else if (noGrouping.equals("-")) {
      value = -1;
    }
    else {
      value = 0;
    }

    return value;
  }

  public static Double getDouble(final String text) {
    if (text == null || text.length() == 0) {
      return null;
    }

    double value;
    if ((text.length() > 0) && (!text.equals("-"))) {
      value = Double.parseDouble(text.replace(',', '.'));
    }
    else if (text.equals("-")) {
      value = -1;
    }
    else {
      value = 0;
    }

    return value;
  }

  public static Long getLong(final String text) {
    if (text == null || text.length() == 0) {
      return null;
    }

    final String noGrouping = text.replace(".", "");

    long value;
    if ((noGrouping.length() > 0) && (!noGrouping.equals("-"))) {
      value = Long.parseLong(noGrouping);
    }
    else if (noGrouping.equals("-")) {
      value = -1;
    }
    else {
      value = 0;
    }

    return value;
  }

  public static void printListContents(final List<?> list) {
    rejectNullValue(list);
    printArrayContents(list.toArray(), false);
  }

  public static void printArrayContents(final Object[] objects) {
    printArrayContents(objects, false);
  }

  public static void printArrayContents(final Object[] objects, boolean onePerLine) {
    System.out.println(getArrayContentsAsString(objects, onePerLine));
  }

  public static String getListContentsAsString(final List<?> list, final boolean onePerLine) {
    if (list == null) {
      return "";
    }

    return getArrayContentsAsString(list.toArray(), onePerLine);
  }

  public static String getArrayContentsAsString(Object[] items, boolean onePerLine) {
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

  public static void printMemoryUsage(final long interval) {
    new Timer(true).scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        System.out.println(getMemoryUsageString());
      }
    }, 0, interval);
  }

  public static long getAllocatedMemory() {
    return Runtime.getRuntime().totalMemory() / 1024;
  }

  public static long getFreeMemory() {
    return Runtime.getRuntime().freeMemory() / 1024;
  }

  public static long getMaxMemory() {
    return Runtime.getRuntime().maxMemory() / 1024;
  }

  public static long getUsedMemory() {
    return getAllocatedMemory() - getFreeMemory();
  }

  public static String getMemoryUsageString() {
    return getUsedMemory() + " KB";
  }

  /**
   * Fetch the entire contents of a resource textfile, and return it in a String, using the default Charset.
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
    rejectNullValue(resourceClass);
    rejectNullValue(resourceName);
    final InputStream inputStream = resourceClass.getResourceAsStream(resourceName);
    if (inputStream == null) {
      throw new FileNotFoundException("Resource not found: '" + resourceName + "'");
    }

    return getTextFileContents(inputStream, charset);
  }

  public static String getTextFileContents(final String filename, final Charset charset) throws IOException {
    rejectNullValue(filename);
    return getTextFileContents(new FileInputStream(new File(filename)), charset);
  }

  public static String getTextFileContents(final InputStream inputStream, final Charset charset) throws IOException {
    rejectNullValue(inputStream);
    final StringBuilder contents = new StringBuilder();
    BufferedReader input = null;
    try {
      input = new BufferedReader(new InputStreamReader(inputStream, charset));
      String line = input.readLine();
      while (line != null) {
        contents.append(line);
        contents.append(System.getProperty("line.separator"));
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

  public static void setClipboard(final String string) {
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(string), null);
  }

  public static String getDelimitedString(final String[][] headers, final String[][] data, final String delimiter) {
    rejectNullValue(headers);
    rejectNullValue(data);
    rejectNullValue(delimiter);
    final StringBuilder contents = new StringBuilder();
    for (final String[] header : headers) {
      for (int j = 0; j < header.length; j++) {
        contents.append(header[j]);
        if (j < header.length - 1) {
          contents.append(delimiter);
        }
      }
      contents.append(System.getProperty("line.separator"));
    }

    for (final String[] someData : data) {
      for (int j = 0; j < someData.length; j++) {
        contents.append(someData[j]);
        if (j < someData.length - 1) {
          contents.append(delimiter);
        }
      }
      contents.append(System.getProperty("line.separator"));
    }

    return contents.toString();
  }

  public static void writeDelimitedFile(final String[][] headers, final String[][] data, final String delimiter,
                                        final File file) {
    writeFile(getDelimitedString(headers, data, delimiter), file);
  }

  public static void writeFile(final String contents, final File file) {
    writeFile(contents, file, false);
  }

  public static void writeFile(final String contents, final File file, final boolean append) {
    rejectNullValue(contents);
    rejectNullValue(file);
    BufferedWriter writer = null;
    try {
      final FileWriter fileWriter = new FileWriter(file, append);
      writer = new BufferedWriter(fileWriter);
      writer.write(contents);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
    finally {
      closeSilently(writer);
    }
  }

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

  public static double roundDouble(final double d, final int places) {
    return Math.round(d * Math.pow(10, (double) places)) / Math.pow(10, (double) places);
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
    rejectNullValue(file);
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
    final StringBuilder sb = new StringBuilder();
    final int length = RANDOM.nextInt(maxLength - minLength) + minLength;
    for( int i = 0; i < length; i++ ) {
      sb.append(AB.charAt(RANDOM.nextInt(AB.length())));
    }

    return sb.toString();
  }

  public static void collate(final List<?> values) {
    Collections.sort(values, new Comparator<Object>() {
      private final Collator collator = Collator.getInstance();
      public int compare(final Object o1, final Object o2) {
        return collator.compare(o1.toString(), o2.toString());
      }
    });
  }

  public static Random getRandom() {
    return RANDOM;
  }

  public static URI getURI(final String urlOrPath) throws URISyntaxException {
    return getURIs(new String[] {urlOrPath})[0];
  }

  public static URI[] getURIs(final String[] urlsOrPaths) throws URISyntaxException {
    final URI[] urls = new URI[urlsOrPaths.length];
    for (int i = 0; i < urlsOrPaths.length; i++) {
      if (urlsOrPaths[i].toLowerCase().startsWith("http")) {
        urls[i] = new URI(urlsOrPaths[i].trim());
      }
      else {
        urls[i] = new File(urlsOrPaths[i].trim()).toURI();
      }
    }

    return urls;
  }

  public static ThreadLocal<Collator> getThreadLocalCollator() {
    return new ThreadLocal<Collator>() {
      @Override
      protected synchronized Collator initialValue() {
        return Collator.getInstance();
      }
    };
  }

  /**
   * Throws an IllegalArgumentException if <code>value</code> is null
   * @param value the value to check
   * @throws IllegalArgumentException if value is null
   */
  public static void rejectNullValue(final Object value) {
    if (value == null) {
      throw new IllegalArgumentException("Value is null");
    }
  }

  /**
   * Throws an IllegalArgumentException with the given message if <code>value</code> is null
   * @param value the value to check
   * @param valueName the name of the null value
   * @throws IllegalArgumentException if value is null
   */
  public static void rejectNullValue(final Object value, final String valueName) {
    if (value == null) {
      throw new IllegalArgumentException(valueName + " is null");
    }
  }

  public static void closeSilently(final ResultSet... resultSets) {
    if (resultSets == null) {
      return;
    }
    for (final ResultSet resultSet : resultSets) {
      try {
        if (resultSet != null) {
          resultSet.close();
        }
      }
      catch (SQLException e) {/**/}
    }
  }

  public static void closeSilently(final Statement... statements) {
    if (statements == null) {
      return;
    }
    for (final Statement statement : statements) {
      try {
        if (statement != null) {
          statement.close();
        }
      }
      catch (SQLException e) {/**/}
    }
  }

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
      catch (Exception e) {/**/}
    }
  }

  /**
   * Maps the given values according to the keys provided by the given key provider.
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
   * @return a map with the values hashed by their respective key values
   */
  public static <K, V> Map<K, Collection<V>> map(final Collection<V> values, final HashKeyProvider<K, V> keyProvider) {
    rejectNullValue(values);
    rejectNullValue(keyProvider);
    final Map<K, Collection<V>> map = new HashMap<K, Collection<V>>(values.size());
    for (final V value : values) {
      map(map, value, keyProvider.getKey(value));
    }

    return map;
  }

  private static <K, V> void map(final Map<K, Collection<V>> map, final V value, final K key) {
    rejectNullValue(value);
    rejectNullValue(key);
    rejectNullValue(map);
    if (!map.containsKey(key)) {
      map.put(key, new ArrayList<V>());
    }

    map.get(key).add(value);
  }

  public static boolean onClasspath(final String classname) {
    rejectNullValue(classname);
    try {
      Class.forName(classname);
      return true;
    }
    catch (ClassNotFoundException e) {
      return false;
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
}