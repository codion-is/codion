/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 *
 */
package org.jminor.common.model;


import org.jminor.common.Constants;
import org.jminor.common.model.formats.LongDateFormat;
import org.jminor.common.model.formats.ShortDashDateFormat;

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
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

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

  private static Level defaultLoggingLevel;

  static {
    final String loggingLevel = System.getProperty(LOGGING_LEVEL_PROPERTY, LOGGING_LEVEL_INFO);
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

  private static final ArrayList<Logger> loggers = new ArrayList<Logger>();

  private Util() {}

  public static String formatLatitude(final String latitude) {
    if (latitude == null || latitude.length() == 0)
      return "";

    String padded = padString(latitude, 6, '0');

    return padded.substring(0, 2) + '\'' + padded.substring(2, 4) + ',' + padded.substring(4, 6) + 'N';
  }

  public static String formatLongitude(final String latitude) {
    if (latitude == null || latitude.length() == 0)
      return "";

    String padded = padString(latitude, 6, '0');

    return padded.substring(0, 2) + '\'' + padded.substring(2, 4) + ',' + padded.substring(4, 6) + 'W';
  }

  public static String padString(final String orig, int length, char padChar) {
    if (orig.length() == length)
      return orig;

    final StringBuffer ret = new StringBuffer(orig);
    while (ret.length() < length)
      ret.append(padChar);

    return ret.toString();
  }

  /**
   * @return Value for property 'loggingLevel'.
   */
  public static Level getLoggingLevel() {
    if (loggers.size() == 0)
      return defaultLoggingLevel;

    return loggers.get(0).getLevel();
  }

  public static void setDefaultLoggingLevel(Level defaultLoggingLevel) {
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

  public static int getInt(String text) {
    if (text == null || text.length() == 0)
      return Constants.INT_NULL_VALUE;

    int value;
    if ((text.length() > 0) && (!text.equals("-")))
      value = Integer.parseInt(text);
    else if (text.equals("-"))
      value = -1;
    else
      value = 0;
    return value;
  }

  public static double getDouble(String text) {
    if (text == null || text.length() == 0)
      return Constants.DOUBLE_NULL_VALUE;

    double value;
    if ((text.length() > 0) && (!text.equals("-"))) {
      value = new Double(text);
    }
    else if (text.equals("-"))
      value = -1;
    else
      value = 0;

    return value;
  }

  public static long getLong(String text) {
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
    System.out.println(getArrayContents(objects, onePerLine));
  }

  public static String getListContents(final List<?> list, final boolean onePerLine) {
    return getArrayContents(list.toArray(), onePerLine);
  }

  public static String getArrayContents(Object[] items, boolean onePerLine) {
    if (items == null)
      return "";

    final StringBuffer ret = new StringBuffer();
    for (int i = 0; i < items.length; i++) {
      final Object item = items[i];
      if (item instanceof Object[])
        ret.append(getArrayContents((Object[]) item, onePerLine));
      else if (!onePerLine)
        ret.append(item).append(i < items.length-1 ? ", " : "");
      else
        ret.append(item).append("\n");
    }

    return ret.toString();
  }

  public static boolean isDateOk(final String date) {
    return isDateOk(date, false);
  }

  public static boolean isDateOk(final String date, final boolean emptyStringOk) {
    return isDateOk(date, emptyStringOk, false);
  }

  public static boolean isDateOk(final String date, final boolean emptyStringOk,
                                 final boolean longFormat) {
    if (longFormat)
      return isDateOk(date, emptyStringOk, LongDateFormat.get());
    else
      return isDateOk(date, emptyStringOk, ShortDashDateFormat.get());
  }

  public static boolean isDateOk(final String date, final boolean emptyStringOk,
                                 final DateFormat format) {
    return isDateOk(date, emptyStringOk, new DateFormat[] {format});
  }

  public static boolean isDateOk(final String date, final boolean emptyStringOk,
                                 final DateFormat[] formats) {
    if (date == null || date.length() == 0)
      return emptyStringOk;

    for (DateFormat format : formats) {
      format.setLenient(false);
      try {
        format.parse(date);
        return true;
      }
      catch (ParseException e) {
        //
      }
    }

    return false;
  }

  public static void printMemoryUsage(final long interval) {
    new Timer(true).scheduleAtFixedRate(new TimerTask() {
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
   * @throws IOException if an IOException should occur
   */
  public static String getContents(final Class cls, final String resourceName) throws IOException {
    final StringBuffer contents = new StringBuffer();
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

  /**
   * @param timestamp the Timestamp object to floor
   * @return a Timestamp object with the same time as <code>timestamp</code>
   * except the Calendar.SECOND and Calendar.MILLISECOND fields are set to zero
   */
  public static Timestamp floorLongDate(Timestamp timestamp) {
    final Calendar cal = Calendar.getInstance();
    cal.setTime(timestamp);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    timestamp = new Timestamp(cal.getTimeInMillis());

    return timestamp;
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
    final StringBuffer propsString = new StringBuffer();
    for (String key : orderedPropertyNames)
      propsString.append(key).append(": ").append(props.getProperty(key)).append("\n");

    return propsString.toString();
  }

  public static void setClipboard(final String string) {
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(string), null);
  }

  public static void writeDelimitedFile(final String[][] headers, final String[][] data,
                                        final String delimiter, final File file) throws UserException {
    final StringBuffer contents = new StringBuffer();
    for (String[] header : headers) {
      for (int j = 0; j < header.length; j++) {
        contents.append(header[j]);
        if (j < header.length - 1)
          contents.append(delimiter);
      }
      contents.append(System.getProperty("line.separator"));
    }

    for (String[] someData : data) {
      for (int j = 0; j < someData.length; j++) {
        contents.append(someData[j]);
        if (j < someData.length - 1)
          contents.append(delimiter);
      }
      contents.append(System.getProperty("line.separator"));
    }
    writeFile(contents.toString(), file);
  }

  public static void writeFile(final String contents, final File file) throws UserException {
    writeFile(contents, file, false);
  }

  public static void writeFile(final String contents, final File file, final boolean append) throws UserException {
    BufferedWriter writer = null;
    try {
      final FileWriter fileWriter = new FileWriter(file, append);
      writer = new BufferedWriter(fileWriter);
      writer.write(contents);
      writer.flush();
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new UserException(e);
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

  public static String sqlEscapeString(final String val) {
    return val.replaceAll("'", "''");
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
}