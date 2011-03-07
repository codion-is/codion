/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
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
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.prefs.Preferences;

/**
 * A static utility class.
 */
public final class Util {

  public static final String VERSION_FILE = "version.txt";

  public static final String PREF_DEFAULT_USERNAME = "jminor.username";
  public static final String LINE_SEPARATOR = System.getProperty("line.separator");

  private static final Logger LOG = LoggerFactory.getLogger(Util.class);
  private static final Random RANDOM = new Random();
  private static final int K = 1024;
  private static final String SPACE = " ";
  private static final String UNDERSCORE = "_";
  private static Preferences userPreferences;

  private Util() {}

  /**
   * Specifies the configuration file name to search for and parse at startup, relative to user.dir,
   * this is done last, so that settings in the configuration file override settings
   * gotten via runtime parameters<br>
   * Value type: String<br>
   * Default value: null
   */
  public static final String CONFIGURATION_FILE = "jminor.configurationFile";

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
    if (userPreferences == null) {
      userPreferences = Preferences.userRoot();
    }
    return userPreferences.get(key, defaultValue);
  }

  public static void putUserPreference(final String key, final String value) {
    rejectNullValue(key, "key");
    if (userPreferences == null) {
      userPreferences = Preferences.userRoot();
    }
    userPreferences.put(key, value);
  }

  public static String getDefaultUserName(final String applicationIdentifier, final String defaultName) {
    rejectNullValue(applicationIdentifier, "applicationIdentifier");
    return getUserPreference(applicationIdentifier + "." + PREF_DEFAULT_USERNAME, defaultName);
  }

  public static void setDefaultUserName(final String applicationClassName, final String username) {
    rejectNullValue(applicationClassName, "applicationClassName");
    putUserPreference(applicationClassName + "." + PREF_DEFAULT_USERNAME, username);
  }

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

  public static Integer getInt(final String text) {
    if (nullOrEmpty(text)) {
      return null;
    }

    final String noGrouping = text.replace(".", "");

    final int value;
    if (!noGrouping.isEmpty() && !noGrouping.equals("-")) {
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
    if (nullOrEmpty(text)) {
      return null;
    }

    final double value;
    if (!text.isEmpty() && !text.equals("-")) {
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
    if (nullOrEmpty(text)) {
      return null;
    }

    final String noGrouping = text.replace(".", "");

    final long value;
    if (!noGrouping.isEmpty() && !noGrouping.equals("-")) {
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
    rejectNullValue(list, "list");
    printArrayContents(list.toArray(), false);
  }

  public static void printArrayContents(final Object[] objects) {
    printArrayContents(objects, false);
  }

  public static void printArrayContents(final Object[] objects, final boolean onePerLine) {
    System.out.println(getArrayContentsAsString(objects, onePerLine));
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

  public static void printMemoryUsage(final long interval) {
    new Timer(true).scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        System.out.println(getMemoryUsageString());
      }
    }, 0, interval);
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
   */
  public static int countLines(final String filename) {
    return countLines(new File(filename));
  }

  /**
   * @param file the file
   * @return the number of lines in the given file
   */
  public static int countLines(final File file) {
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader(file));
      int lines = 0;
      while (reader.readLine() != null) {
        lines++;
      }

      return lines;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
    finally {
      Util.closeSilently(reader);
    }
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

  public static void setClipboard(final String string) {
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(string), null);
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
                                        final File file) {
    writeFile(getDelimitedString(headers, data, delimiter), file);
  }

  public static void writeFile(final String contents, final File file) {
    writeFile(contents, file, false);
  }

  public static void writeFile(final String contents, final File file, final boolean append) {
    rejectNullValue(contents, "contents");
    rejectNullValue(file, "file");
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

  public static List<Object> deserializeFromFile(final File file) {
    final List<Object> objects = new ArrayList<Object>();
    ObjectInputStream inputStream = null;
    try {
      inputStream = new ObjectInputStream(new FileInputStream(file));
      while (true) {
        objects.add(inputStream.readObject());
      }
    }
    catch (EOFException ex) {/**/}
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
    final StringBuilder sb = new StringBuilder();
    final int length = RANDOM.nextInt(maxLength - minLength) + minLength;
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
   * Closes the given ResultSet instances, swallowing any SQLExceptions that occur
   * @param resultSets the result sets to close
   */
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

  /**
   * Closes the given Statement instances, swallowing any SQLExceptions that occur
   * @param statements the statements to close
   */
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
    rejectNullValue(values, "values");
    rejectNullValue(keyProvider, "keyProvider");
    final Map<K, Collection<V>> map = new HashMap<K, Collection<V>>(values.size());
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
   * @param strings the strings to check
   * @return true if one of the given strings is null or empty, false otherwise
   */
  public static boolean nullOrEmpty(final String... strings) {
    if (strings == null) {
      return true;
    }
    for (final String string : strings) {
      if (string == null || string.isEmpty()) {
        return true;
      }
    }

    return false;
  }

  /**
   * @param sqlType the type
   * @return the Class representing the given type
   */
  public static Class<?> getTypeClass(final int sqlType) {
    switch (sqlType) {
      case Types.INTEGER:
        return Integer.class;
      case Types.DOUBLE:
        return Double.class;
      case Types.DATE:
        return Date.class;
      case Types.TIMESTAMP:
        return Timestamp.class;
      case Types.VARCHAR:
        return String.class;
      case Types.BOOLEAN:
        return Boolean.class;
      case Types.CHAR:
        return Character.class;

      default:
        return Object.class;
    }
  }

  /**
   * Parses the configuration file specified by the CONFIGURATION_FILE property
   * @see #CONFIGURATION_FILE
   */
  public static void parseConfigurationFile() {
    final String filename = System.getProperty(CONFIGURATION_FILE);
    if (filename != null) {
      InputStream inputStream = null;
      try {
        inputStream = ClassLoader.getSystemResourceAsStream(filename);
        if (inputStream == null) {
          throw new RuntimeException("Unable to load configuration file: " + filename);
        }
        LOG.debug("Reading configuration file: " + filename);
        final Properties properties = new Properties();
        properties.load(inputStream);
        for (final Map.Entry entry : properties.entrySet()) {
          final Object key = entry.getKey();
          LOG.debug(key + " - > " + properties.get(key));
          System.setProperty((String) key, (String) properties.get(key));
        }
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
      finally {
        closeSilently(inputStream);
      }
    }
  }

  /**
   * Initializes a Registry if one is not running
   * @param port the port on which to look for (or create) a registry
   * @throws RemoteException in case of an exception
   */
  public static void initializeRegistry(final int port) throws RemoteException {
    final Registry localRegistry = getRegistry(port);
    try {
      localRegistry.list();
    }
    catch (Exception e) {
      LOG.debug("Trying to locate registry on server start", e);
      LOG.info("Creating registry on port: " + port);
      LocateRegistry.createRegistry(port);
    }
  }

  /**
   * @param port the port on which to look for a registry
   * @return the registry
   * @throws java.rmi.RemoteException in case of an exception
   */
  public static Registry getRegistry(final int port) throws RemoteException {
    return LocateRegistry.getRegistry(port);
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
    if (logger != null) {
      logger.error(exception.getMessage(), exception);
    }

    return exception;
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
}