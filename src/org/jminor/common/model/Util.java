/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.jminor.framework.FrameworkSettings;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import javax.swing.TransferHandler;
import javax.swing.text.JTextComponent;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
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

  private static final DateFormat DEFAULT_SHORT_DATE_FORMAT = new SimpleDateFormat((String)
          FrameworkSettings.get().getProperty(FrameworkSettings.DEFAULT_SHORT_DATE_FORMAT));
  private static final DateFormat DEFAULT_LONG_DATE_FORMAT = new SimpleDateFormat((String)
          FrameworkSettings.get().getProperty(FrameworkSettings.DEFAULT_LONG_DATE_FORMAT));

  private static Level defaultLoggingLevel;
  private static final Preferences USER_PREFERENCES = Preferences.userRoot();

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

  private static final ArrayList<Logger> loggers = new ArrayList<Logger>();

  public static String getUserPreference(final String key, final String defaultValue) {
    return USER_PREFERENCES.get(key, defaultValue);
  }

  public static void putUserPreference(final String key, final String value) {
    USER_PREFERENCES.put(key, value);
  }

  public static String getDefaultUserName(final String applicationIdentifier, final String defaultName) {
    return getUserPreference(applicationIdentifier + "." + PREF_DEFAULT_USERNAME, defaultName);
  }

  public static void setDefaultUserName(final String applicationClassname, final String username) {
    putUserPreference(applicationClassname + "." + PREF_DEFAULT_USERNAME, username);
  }

  public static String formatLatitude(final String latitude) {
    if (latitude == null || latitude.length() == 0)
      return "";

    final boolean minus = latitude.startsWith("-");
    String padded = padString(latitude.replaceAll("-", ""), 6, '0', false);

    return padded.substring(0, 2) + '\'' + padded.substring(2, 4) + ',' + padded.substring(4, 6) + (minus ? 'S' : 'N');
  }

  public static String formatLongitude(final String longitude) {
    if (longitude == null || longitude.length() == 0)
      return "";

    final boolean minus = longitude.startsWith("-");
    String padded = padString(longitude.replaceAll("-", ""), 6, '0', false);

    return padded.substring(0, 2) + '\'' + padded.substring(2, 4) + ',' + padded.substring(4, 6) + (minus ? 'W' : 'E');
  }

  public static String padString(final String orig, final int length,
                                 final char padChar, final boolean atFront) {
    if (orig.length() == length)
      return orig;

    final StringBuffer ret = new StringBuffer(orig);
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

    final StringBuffer ret = new StringBuffer();
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

  /**
   * @param date the date to check for validity
   * @return true if the date is valid, using the default short date format
   * @see FrameworkSettings#DEFAULT_SHORT_DATE_FORMAT
   */
  public static boolean isDateValid(final String date) {
    return isDateValid(date, false);
  }

  /**
   * @param date the date to check for validity
   * @param emptyStringOk if true then an empty string is regarded as a valid date
   * @return true if the date is valid, using the default short date format
   * @see FrameworkSettings#DEFAULT_SHORT_DATE_FORMAT
   */
  public static boolean isDateValid(final String date, final boolean emptyStringOk) {
    return isDateValid(date, emptyStringOk, false);
  }

  /**
   * @param date the date to check for validity
   * @param emptyStringOk if true then an empty string is regarded as a valid date
   * @param longFormat if true then the default long date format is used, otherwise
   * the default short date format is used
   * @return true if the date is valid, using the default date format
   * @see FrameworkSettings#DEFAULT_SHORT_DATE_FORMAT
   * @see FrameworkSettings#DEFAULT_LONG_DATE_FORMAT
   */
  public static boolean isDateValid(final String date, final boolean emptyStringOk,
                                    final boolean longFormat) {
    if (longFormat)
      return isDateValid(date, emptyStringOk, DEFAULT_LONG_DATE_FORMAT);
    else
      return isDateValid(date, emptyStringOk, DEFAULT_SHORT_DATE_FORMAT);
  }

  /**
   * @param date the date to check for validity
   * @param emptyStringOk if true then an empty string is regarded as a valid date
   * @param format the date format to use for validation
   * @return true if the date is valid, using the given date format
   */
  public static boolean isDateValid(final String date, final boolean emptyStringOk,
                                    final DateFormat format) {
    return isDateValid(date, emptyStringOk, new DateFormat[] {format});
  }

  /**
   * @param date the date to check for validity
   * @param emptyStringOk if true then an empty string is regarded as a valid date
   * @param formats the date formats to use for validation
   * @return true if the date is valid, using the given date format
   */
  public static boolean isDateValid(final String date, final boolean emptyStringOk,
                                    final DateFormat[] formats) {
    if (date == null || date.length() == 0)
      return emptyStringOk;

    for (final DateFormat format : formats) {
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
  public static Timestamp floorLongDate(final Timestamp timestamp) {
    final Calendar cal = Calendar.getInstance();
    cal.setTime(timestamp);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);

    return new Timestamp(cal.getTimeInMillis());
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
    for (final String key : orderedPropertyNames)
      propsString.append(key).append(": ").append(props.getProperty(key)).append("\n");

    return propsString.toString();
  }

  public static void setClipboard(final String string) {
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(string), null);
  }

  public static String getDelimitedString(final String[][] headers, final String[][] data,
                                          final String delimiter) throws UserException {
    final StringBuffer contents = new StringBuffer();
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

  public static void writeDelimitedFile(final String[][] headers, final String[][] data,
                                        final String delimiter, final File file) throws UserException {
    writeFile(getDelimitedString(headers, data, delimiter), file);
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
    catch (IOException e) {
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

  public static Date getYesterday() {
    final Calendar c = Calendar.getInstance();
    c.add(Calendar.DAY_OF_MONTH, -1);

    return c.getTime();
  }

  public static Date getFirstDayOfLastMonth() {
    final Calendar c = Calendar.getInstance();
    c.add(Calendar.MONTH, -1);
    c.set(Calendar.DAY_OF_MONTH, 1);

    return c.getTime();
  }

  public static Date getLastDayOfLastMonth() {
    final Calendar c = Calendar.getInstance();
    c.add(Calendar.MONTH, -1);
    c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));

    return c.getTime();
  }

  public static Date getFirstDayOfMonth(final int toAdd) {
    final Calendar c = Calendar.getInstance();
    c.add(Calendar.MONTH, toAdd);
    c.set(Calendar.DAY_OF_MONTH, 1);

    return c.getTime();
  }

  public static Date getFirstDayOfYear() {
    final Calendar c = Calendar.getInstance();
    c.set(Calendar.MONTH, Calendar.JANUARY);
    c.set(Calendar.DAY_OF_MONTH, 1);

    return c.getTime();
  }

  public static Date getLastDayOfYear() {
    final Calendar c = Calendar.getInstance();
    c.set(Calendar.MONTH, Calendar.DECEMBER);
    c.set(Calendar.DAY_OF_MONTH, 31);

    return c.getTime();
  }

  public static Date getFirstDayOfQuarter(final int quarter) {
    if (quarter < 1 || quarter > 4)
      throw new IllegalArgumentException("Quarter must be between 1 and 4");

    final Calendar c = Calendar.getInstance();
    c.set(Calendar.DAY_OF_MONTH, 1);
    switch (quarter) {
      case 1: {
        c.set(Calendar.MONTH, Calendar.JANUARY);
        return c.getTime();
      }
      case 2: {
        c.set(Calendar.MONTH, Calendar.APRIL);
        return c.getTime();
      }
      case 3: {
        c.set(Calendar.MONTH, Calendar.JULY);
        return c.getTime();
      }
      case 4: {
        c.set(Calendar.MONTH, Calendar.OCTOBER);
        return c.getTime();
      }
    }

    return null;
  }

  public static Date getLastDayOfQuarter(final int quarter) {
    if (quarter < 1 || quarter > 4)
      throw new IllegalArgumentException("Quarter must be between 1 and 4");

    final Calendar c = Calendar.getInstance();
    switch (quarter) {
      case 1: {
        c.set(Calendar.MONTH, Calendar.MARCH);
        c.set(Calendar.DAY_OF_MONTH, 31);
        return c.getTime();
      }
      case 2: {
        c.set(Calendar.MONTH, Calendar.JUNE);
        c.set(Calendar.DAY_OF_MONTH, 30);
        return c.getTime();
      }
      case 3: {
        c.set(Calendar.MONTH, Calendar.SEPTEMBER);
        c.set(Calendar.DAY_OF_MONTH, 30);
        return c.getTime();
      }
      case 4: {
        c.set(Calendar.MONTH, Calendar.DECEMBER);
        c.set(Calendar.DAY_OF_MONTH, 31);
        return c.getTime();
      }
    }

    return null;
  }

  public static double roundDouble(final double d, final int places) {
    return Math.round(d * Math.pow(10, (double) places)) / Math.pow(10, (double) places);
  }

  public static int numberOfDaysInRange(final Date from, final Date to) {
    final Calendar fromCalendar = Calendar.getInstance();
    fromCalendar.setTime(from);
    final Calendar toCalendar = Calendar.getInstance();
    toCalendar.setTime(to);
    if (fromCalendar.after(toCalendar))
      throw new IllegalArgumentException("'To' date should be after 'from' date");

    int numberOfDays = 0;
    while (fromCalendar.before(toCalendar)) {
      numberOfDays++;
      fromCalendar.add(Calendar.DAY_OF_YEAR, 1);
    }
    numberOfDays++;

    return numberOfDays;
  }

  /**
   * Makes the text component accept files during drag and drop operations and
   * insert the absolute path of the dropped file (the first file in a list if more
   * than one file is dropped)
   * @param textComponent the text component
   */
  public static void addAcceptSingleFileDragAndDrop(final JTextComponent textComponent) {
    textComponent.setDragEnabled(true);
    textComponent.setTransferHandler(new TransferHandler() {
      @Override
      public boolean canImport(final TransferSupport support) {
        try {
          final DataFlavor nixFileDataFlavor = new DataFlavor("text/uri-list;class=java.lang.String");
          for (final DataFlavor flavor : support.getDataFlavors())
            if (flavor.isFlavorJavaFileListType() || flavor.equals(nixFileDataFlavor))
              return true;

          return false;
        }
        catch (ClassNotFoundException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public boolean importData(final TransferSupport support) {
        final String path = getFileDataFlavor(support);
        if (path != null) {
          textComponent.setText(path);
          return true;
        }
        else
          return false;
      }
    });
  }

  @SuppressWarnings({"unchecked"})
  private static String getFileDataFlavor(final TransferHandler.TransferSupport support) {
    try {
      for (final DataFlavor flavor : support.getDataFlavors()) {
        if (flavor.isFlavorJavaFileListType()) {
          final List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

          return files.size() > 0 ? files.get(0).getAbsolutePath() : null;
        }
      }
      //the code below is for handling unix/linux
      final DataFlavor nixFileDataFlavor = new DataFlavor("text/uri-list;class=java.lang.String");
      final String data = (String) support.getTransferable().getTransferData(nixFileDataFlavor);
      for(final StringTokenizer st = new StringTokenizer(data, "\r\n"); st.hasMoreTokens();) {
        final String token = st.nextToken().trim();
        if(token.startsWith("#") || token.isEmpty()) // comment line, by RFC 2483
          continue;

        return new File(new URI(token)).getAbsolutePath();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }

    return null;
  }
}