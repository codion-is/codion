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
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ThreadFactory;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * A static utility class.
 */
public final class Util extends org.jminor.common.Util {

  /**
   * The name of the preferences key used to save the default username
   */
  public static final String PREFERENCE_DEFAULT_USERNAME = "jminor.username";
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
  private static final String SPACE = " ";
  private static final String UNDERSCORE = "_";
  private static final int INPUT_BUFFER_SIZE = 8192;
  private static final int TEN = 10;
  private static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private static Preferences userPreferences;

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

  private Util() {}

  /**
   * Returns true if the given host is reachable, false if it is not or an exception is thrown while trying
   * @param host the hostname
   * @param timeout the timeout in milliseconds
   * @return true if the host is reachable
   */
  public static boolean isHostReachable(final String host, final int timeout) {
    Objects.requireNonNull(host, "host");
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
    Objects.requireNonNull(key, KEY);
    return getUserPreferences().get(key, defaultValue);
  }

  /**
   * @param key the key to use to identify the preference
   * @param value the preference value to associate with the given key
   */
  public static void putUserPreference(final String key, final String value) {
    Objects.requireNonNull(key, KEY);
    getUserPreferences().put(key, value);
  }

  /**
   * Removes the preference associated with the given key
   * @param key the key to use to identify the preference to remove
   */
  public static void removeUserPreference(final String key) {
    Objects.requireNonNull(key, KEY);
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
    Objects.requireNonNull(applicationIdentifier, "applicationIdentifier");
    return getUserPreference(applicationIdentifier + "." + PREFERENCE_DEFAULT_USERNAME, defaultName);
  }

  /**
   * Saves the default username for the given application identifier
   * @param applicationIdentifier the application identifier
   * @param username the username
   */
  public static void setDefaultUserName(final String applicationIdentifier, final String username) {
    Objects.requireNonNull(applicationIdentifier, "applicationIdentifier");
    putUserPreference(applicationIdentifier + "." + PREFERENCE_DEFAULT_USERNAME, username);
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
    if ("-".equals(noGrouping)) {
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

    if ("-".equals(text)) {
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
    if ("-".equals(noGrouping)) {
      return  -1L;
    }

    return Long.parseLong(noGrouping);
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
    Objects.requireNonNull(headers, "headers");
    Objects.requireNonNull(data, "data");
    Objects.requireNonNull(delimiter, "delimiter");
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

    for (int i = 0; i < data.length; i++) {
      final String[] someData = data[i];
      for (int j = 0; j < someData.length; j++) {
        contents.append(someData[j]);
        if (j < someData.length - 1) {
          contents.append(delimiter);
        }
      }
      if (i < someData.length) {
        contents.append(LINE_SEPARATOR);
      }
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
    Objects.requireNonNull(contents, "contents");
    Objects.requireNonNull(file, "file");
    try (final BufferedWriter writer = new BufferedWriter(new FileWriter(file, append))) {
      writer.write(contents);
    }
  }

  /**
   * Deserializes a list of Objects from the given file
   * @param file the file
   * @return deserialized objects
   * @throws Serializer.SerializeException in case of an exception
   */
  public static List<Object> deserializeFromFile(final File file) throws Serializer.SerializeException {
    final List<Object> objects = new ArrayList<>();
    try (final ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(file))) {
      while (true) {
        objects.add(inputStream.readObject());
      }
    }
    catch (final EOFException ignored) {/*ignored*/}
    catch (final Exception e) {
      throw new Serializer.SerializeException(e.getMessage(), e);
    }

    return objects;
  }

  /**
   * Srializes a Collection of Objects to a given file
   * @param objects the objects to serialize
   * @param file the file
   * @throws Serializer.SerializeException in case of an exception
   */
  public static void serializeToFile(final Collection objects, final File file) throws Serializer.SerializeException {
    try (final ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file))) {
      for (final Object object : objects) {
        outputStream.writeObject(object);
      }
    }
    catch (final IOException e) {
      throw new Serializer.SerializeException(e.getMessage(), e);
    }
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
   * @param file the file
   * @return the bytes comprising the given file
   * @throws IOException in case of an exception
   */
  public static byte[] getBytesFromFile(final File file) throws IOException {
    Objects.requireNonNull(file, "file");
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
    Objects.requireNonNull(collator, "collator");
    Objects.requireNonNull(stringOne, "stringOne");
    Objects.requireNonNull(stringTwo, "stringTwo");

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