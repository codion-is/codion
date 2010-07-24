/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * A static utility class for date handling.<br>
 * User: Bjorn Darri<br>
 * Date: 3.8.2009<br>
 * Time: 00:09:47<br>
 */
public final class DateUtil {

  private static final int THIRTY_FIRST = 31;
  private static final int THIRTIETH = 30;

  private DateUtil() {}

  /**
   * @param date the date to check for validity
   * @param formats the date formats to use for validation
   * @return true if the date is valid
   */
  public static boolean isDateValid(final String date, final DateFormat... formats) {
    return isDateValid(date, false, formats);
  }

  /**
   * @param date the date to check for validity
   * @param emptyStringOk if true then an empty string is regarded as a valid date
   * @param formats the date formats to use for validation
   * @return true if the date is valid, using the given date formats
   */
  public static boolean isDateValid(final String date, final boolean emptyStringOk, final DateFormat... formats) {
    if (formats == null || formats.length == 0) {
      throw new RuntimeException("Date format is required");
    }
    if (date == null || date.length() == 0) {
      return emptyStringOk;
    }

    for (final DateFormat format : formats) {
      format.setLenient(false);
      try {
        format.parse(date);
        return true;
      }
      catch (ParseException e) {/**/}
    }

    return false;
  }

  /**
   * @param date the Date object to floor
   * @return a Date object with the same time as <code>timestamp</code>
   * except the Calendar.SECOND and Calendar.MILLISECOND fields are set to zero
   */
  public static Date floorDate(final Date date) {
    final Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    cal.set(Calendar.MILLISECOND, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.HOUR_OF_DAY, 0);

    return cal.getTime();
  }

  /**
   * @param timestamp the Timestamp object to floor
   * @return a Timestamp object with the same time as <code>timestamp</code>
   * except the Calendar.SECOND and Calendar.MILLISECOND fields are set to zero
   */
  public static Timestamp floorTimestamp(final Timestamp timestamp) {
    final Calendar cal = Calendar.getInstance();
    cal.setTime(timestamp);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);

    return new Timestamp(cal.getTimeInMillis());
  }

  public static Date getDate(int year, int month, int day) {
    final Calendar cal = Calendar.getInstance();
    cal.set(Calendar.YEAR, year);
    cal.set(Calendar.MONTH, month);
    cal.set(Calendar.DAY_OF_MONTH, day);

    return cal.getTime();
  }

  public static Date getYesterday() {
    final Calendar c = Calendar.getInstance();
    c.add(Calendar.DAY_OF_MONTH, -1);

    return c.getTime();
  }

  public static Date getFirstDayOfLastMonth() {
    return getFirstDayOfMonth(-1);
  }

  public static Date getLastDayOfLastMonth() {
    return getLastDayOfMonth(-1);
  }

  public static Date getFirstDayOfMonth(final int toAdd) {
    final Calendar c = Calendar.getInstance();
    c.add(Calendar.MONTH, toAdd);
    c.set(Calendar.DAY_OF_MONTH, 1);

    return c.getTime();
  }

  public static Date getLastDayOfMonth(final int toAdd) {
    final Calendar c = Calendar.getInstance();
    c.add(Calendar.MONTH, toAdd);
    c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));

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
    c.set(Calendar.DAY_OF_MONTH, THIRTY_FIRST);

    return c.getTime();
  }

  public static Date getFirstDayOfQuarter(final int quarter) {
    if (quarter < 1 || quarter > 4) {
      throw new IllegalArgumentException("Quarter must be between 1 and 4");
    }

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
    if (quarter < 1 || quarter > 4) {
      throw new IllegalArgumentException("Quarter must be between 1 and 4");
    }

    final Calendar c = Calendar.getInstance();
    switch (quarter) {
      case 1: {
        c.set(Calendar.MONTH, Calendar.MARCH);
        c.set(Calendar.DAY_OF_MONTH, THIRTY_FIRST);
        return c.getTime();
      }
      case 2: {
        c.set(Calendar.MONTH, Calendar.JUNE);
        c.set(Calendar.DAY_OF_MONTH, THIRTIETH);
        return c.getTime();
      }
      case 3: {
        c.set(Calendar.MONTH, Calendar.SEPTEMBER);
        c.set(Calendar.DAY_OF_MONTH, THIRTIETH);
        return c.getTime();
      }
      case 4: {
        c.set(Calendar.MONTH, Calendar.DECEMBER);
        c.set(Calendar.DAY_OF_MONTH, THIRTY_FIRST);
        return c.getTime();
      }
    }

    return null;
  }

  public static int numberOfDaysInRange(final Date from, final Date to) {
    final Calendar fromCalendar = Calendar.getInstance();
    fromCalendar.setTime(from);
    final Calendar toCalendar = Calendar.getInstance();
    toCalendar.setTime(to);
    if (fromCalendar.after(toCalendar)) {
      throw new IllegalArgumentException("'To' date should be after 'from' date");
    }

    int numberOfDays = 0;
    while (fromCalendar.before(toCalendar)) {
      numberOfDays++;
      fromCalendar.add(Calendar.DAY_OF_YEAR, 1);
    }
    numberOfDays++;

    return numberOfDays;
  }

  public static ThreadLocal<DateFormat> getThreadLocalDateFormat(final String formatString) {
    return new ThreadLocal<DateFormat>() {
      @Override
      protected synchronized DateFormat initialValue() {
        return new SimpleDateFormat(formatString);
      }
    };
  }

  /**
   * Parses the date pattern and returns mask string that can be used in JFormattedFields.
   * This only works with plain numerical date formats.
   * @param format the SimpleDateFormat for which to retrieve the date mask
   * @return a String representing the mask to use in JFormattedTextFields, i.e. "##-##-####"
   */
  public static String getDateMask(final SimpleDateFormat format) {
    final String datePattern = format.toPattern();
    final StringBuilder stringBuilder = new StringBuilder(datePattern.length());
    for (final Character character : datePattern.toCharArray()) {
      stringBuilder.append(Character.isLetter(character) ? "#" : character);
    }

    return stringBuilder.toString();
  }
}
