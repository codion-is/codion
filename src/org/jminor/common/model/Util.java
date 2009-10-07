/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.prefs.Preferences;

/**
 * A static utility class
 */
public class Util {

  public static final String VERSION_FILE = "version.txt";

  public static final String LOGGING_LEVEL_PROPERTY = "jminor.logging.level";
  public static final String LOGGING_LEVEL_INFO = "info";
  public static final String LOGGING_LEVEL_DEBUG = "debug";
  public static final String LOGGING_LEVEL_WARN = "warn";
  public static final String LOGGING_LEVEL_FATAL = "fatal";
  public static final String LOGGING_LEVEL_TRACE = "trace";

  public static final String PREF_DEFAULT_USERNAME = "jminor.username";


  private static List<Logger> loggers = new ArrayList<Logger>();
  private static Level defaultLoggingLevel;
  private static Preferences userPreferences;

  static {
    final String loggingLevel = System.getProperty(LOGGING_LEVEL_PROPERTY, LOGGING_LEVEL_INFO);
    System.out.println("Initial logging level: " + loggingLevel);
    if (loggingLevel.equals(LOGGING_LEVEL_INFO))
      defaultLoggingLevel = Level.INFO;
    else if (loggingLevel.equals(LOGGING_LEVEL_DEBUG))
      defaultLoggingLevel = Level.DEBUG;
    else if (loggingLevel.equals(LOGGING_LEVEL_WARN))
      defaultLoggingLevel = Level.WARN;
    else if (loggingLevel.equals(LOGGING_LEVEL_FATAL))
      defaultLoggingLevel = Level.FATAL;
    else if (loggingLevel.equals(LOGGING_LEVEL_TRACE))
      defaultLoggingLevel = Level.TRACE;
  }

  public static String getUserPreference(final String key, final String defaultValue) {
    if (userPreferences == null)
      userPreferences = Preferences.userRoot();
    return userPreferences.get(key, defaultValue);
  }

  public static void putUserPreference(final String key, final String value) {
    if (userPreferences == null)
      userPreferences = Preferences.userRoot();
    userPreferences.put(key, value);
  }

  public static String getDefaultUserName(final String applicationIdentifier, final String defaultName) {
    return getUserPreference(applicationIdentifier + "." + PREF_DEFAULT_USERNAME, defaultName);
  }

  public static void setDefaultUserName(final String applicationClassname, final String username) {
    putUserPreference(applicationClassname + "." + PREF_DEFAULT_USERNAME, username);
  }

  public static String padString(final String orig, final int length, final char padChar, final boolean atFront) {
    if (orig.length() == length)
      return orig;

    final StringBuilder ret = new StringBuilder(orig);
    while (ret.length() < length) {
      if (atFront)
        ret.insert(0, padChar);
      else
        ret.append(padChar);
    }

    return ret.toString();
  }

  /**
   * @return the current logging level
   */
  public static Level getLoggingLevel() {
    if (loggers.size() == 0)
      return defaultLoggingLevel;

    return loggers.get(0).getLevel();
  }

  public static void setDefaultLoggingLevel(final Level defaultLoggingLevel) {
    Util.defaultLoggingLevel = defaultLoggingLevel;
  }

  public static void setLoggingLevel(final Level level) {
    for (final Logger logger : loggers)
      logger.setLevel(level);
  }

  public static Logger getLogger(final Class classToLog) {
    final Logger ret = Logger.getLogger(classToLog);
    ret.setLevel(getLoggingLevel());
    loggers.add(ret);

    return ret;
  }

  public static Logger getLogger(final String name) {
    final Logger ret = Logger.getLogger(name);
    ret.setLevel(getLoggingLevel());
    loggers.add(ret);

    return ret;
  }

  public static Integer getInt(final String text) {
    if (text == null || text.length() == 0)
      return null;

    int value;
    if ((text.length() > 0) && (!text.equals("-")))
      value = Integer.parseInt(text);
    else if (text.equals("-"))
      value = -1;
    else
      value = 0;

    return value;
  }

  public static Double getDouble(final String text) {
    if (text == null || text.length() == 0)
      return null;

    double value;
    if ((text.length() > 0) && (!text.equals("-")))
      value = Double.parseDouble(text.replace(',', '.'));
    else if (text.equals("-"))
      value = -1;
    else
      value = 0;

    return value;
  }

  public static Long getLong(final String text) {
    if (text == null || text.length() == 0)
      return null;

    long value;
    if ((text.length() > 0) && (!text.equals("-")))
      value = Long.parseLong(text);
    else if (text.equals("-"))
      value = -1;
    else
      value = 0;

    return value;
  }

  public static void printListContents(final List<?> list) {
    printArrayContents(list.toArray(), false);
  }

  public static void printArrayContents(final Object[] objects) {
    printArrayContents(objects, false);
  }

  public static void printArrayContents(final Object[] objects, boolean onePerLine) {
    System.out.println(getArrayContentsAsString(objects, onePerLine));
  }

  public static String getListContentsAsString(final List<?> list, final boolean onePerLine) {
    return getArrayContentsAsString(list.toArray(), onePerLine);
  }

  public static String getArrayContentsAsString(Object[] items, boolean onePerLine) {
    if (items == null)
      return "";

    final StringBuilder ret = new StringBuilder();
    for (int i = 0; i < items.length; i++) {
      final Object item = items[i];
      if (item instanceof Object[])
        ret.append(getArrayContentsAsString((Object[]) item, onePerLine));
      else if (!onePerLine)
        ret.append(item).append(i < items.length-1 ? ", " : "");
      else
        ret.append(item).append("\n");
    }

    return ret.toString();
  }

  public static void printMemoryUsage(final long interval) {
    new Timer(true).scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        System.out.println(getMemoryUsageString());
      }
    }, 0, interval);
  }

  public static String getMemoryUsageString() {
    return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/1024 + " KB";
  }

  /**
   * Fetch the entire contents of a resource file, and return it in a String.
   * @param cls the resource class
   * @param resourceName the name of the resource to retrieve
   * @return the contents of the resource file
   * @throws IOException in case an IOException occurs
   */
  public static String getContents(final Class cls, final String resourceName) throws IOException {
    final StringBuilder contents = new StringBuilder();
    BufferedReader input = null;
    try {
      final InputStream inputStream = cls.getResourceAsStream(resourceName);
      if (inputStream == null)
        throw new FileNotFoundException("File not found: '" + resourceName + "'");

      input = new BufferedReader(new InputStreamReader(inputStream));
      String line;
      while (( line = input.readLine()) != null) {
        contents.append(line);
        contents.append(System.getProperty("line.separator"));
      }
    }
    finally {
      try {
        if (input!= null)
          input.close();
      }
      catch (IOException ex) {
        ex.printStackTrace();
      }
    }

    return contents.toString();
  }

  public static String getSystemProperties() {
    try {
      final SecurityManager manager = System.getSecurityManager();
      if (manager != null)
        manager.checkPropertiesAccess();
    }
    catch (SecurityException e) {
      return "";
    }
    final Properties props = System.getProperties();
    final Enumeration propNames = props.propertyNames();
    final ArrayList<String> orderedPropertyNames = new ArrayList<String>(props.size());
    while (propNames.hasMoreElements())
      orderedPropertyNames.add((String) propNames.nextElement());

    Collections.sort(orderedPropertyNames);
    final StringBuilder propsString = new StringBuilder();
    for (final String key : orderedPropertyNames)
      propsString.append(key).append(": ").append(props.getProperty(key)).append("\n");

    return propsString.toString();
  }

  public static void setClipboard(final String string) {
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(string), null);
  }

  public static String getDelimitedString(final String[][] headers, final String[][] data, final String delimiter) {
    final StringBuilder contents = new StringBuilder();
    for (final String[] header : headers) {
      for (int j = 0; j < header.length; j++) {
        contents.append(header[j]);
        if (j < header.length - 1)
          contents.append(delimiter);
      }
      contents.append(System.getProperty("line.separator"));
    }

    for (final String[] someData : data) {
      for (int j = 0; j < someData.length; j++) {
        contents.append(someData[j]);
        if (j < someData.length - 1)
          contents.append(delimiter);
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
    BufferedWriter writer = null;
    try {
      final FileWriter fileWriter = new FileWriter(file, append);
      writer = new BufferedWriter(fileWriter);
      writer.write(contents);
      writer.flush();
    }
    catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    finally {
      try {
        if (writer != null)
          writer.close();
      }
      catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public static boolean equal(final Object one, final Object two) {
    return one == null && two == null || !(one == null ^ two == null) && one.equals(two);
  }

  public static String getVersion() {
    final String ret = getVersionAndBuildNumber();
    if (ret.toLowerCase().contains("build"))
      return ret.substring(0, ret.toLowerCase().indexOf("build")-1);

    return "N/A";
  }

  public static String getVersionAndBuildNumber() {
    try {
      return getContents(Util.class, VERSION_FILE);
    }
    catch (IOException e) {
      e.printStackTrace();
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
    if (objects == null)
      return false;
    for (final Object object : objects)
      if (object == null)
          return false;

    return true;
  }
}