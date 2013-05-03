/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * A static utility class for date handling.<br>
 */
public final class DateUtil {

  public enum Quarter {
    FIRST, SECOND, THIRD, FOURTH
  }

  private static final int THIRTY_FIRST = 31;
  private static final int THIRTIETH = 30;
  private static final int NINETEENSEVENTY = 1970;

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
   * @param dateString the date to check for validity
   * @param emptyStringOk if true then an empty string is regarded as a valid date
   * @param formats the date formats to use for validation
   * @return true if the date is valid, using the given date formats
   */
  public static boolean isDateValid(final String dateString, final boolean emptyStringOk, final DateFormat... formats) {
    if (formats == null || formats.length == 0) {
      throw new IllegalArgumentException("Date format is required");
    }
    if (Util.nullOrEmpty(dateString)) {
      return emptyStringOk;
    }

    for (final DateFormat format : formats) {
      format.setLenient(false);
      try {
        format.parse(dateString);
        return true;
      }
      catch (ParseException ignored) {}
    }

    return false;
  }

  /**
   * @param date the Date object to floor
   * @return a Time object with the same time of day as <code>date</code>, except the Calendar.YEAR,
   * Calendar.MONTH and Calendar.DATE fields are set to 1970, january and 1 respectively
   */
  public static Time floorTime(final Date date) {
    final Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    cal.set(Calendar.YEAR, NINETEENSEVENTY);
    cal.set(Calendar.MONTH, Calendar.JANUARY);
    cal.set(Calendar.DATE, 1);

    return new Time(cal.getTimeInMillis());
  }

  /**
   * @param date the Date object to floor
   * @return a Date object with the same date as <code>date</code>, except the Calendar.HOUR_OF_DAY,
   * Calendar.MINUTE, Calendar.SECOND and Calendar.MILLISECOND fields are set to zero
   */
  public static Date floorDate(final Date date) {
    final Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    floorFields(cal, Calendar.MILLISECOND, Calendar.SECOND, Calendar.MINUTE, Calendar.HOUR_OF_DAY);

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
    floorFields(cal, Calendar.SECOND, Calendar.MILLISECOND);

    return new Timestamp(cal.getTimeInMillis());
  }

  /**
   * Floors the time fields in the given calendar, Calendar.HOUR_OF_DAY,
   * Calendar.MINUTE, Calendar.SECOND and Calendar.MILLISECOND
   * @param calendar the calendar in which to floor the time fields
   */
  public static void floorTimeFields(final Calendar calendar) {
    floorFields(calendar, Calendar.MILLISECOND, Calendar.SECOND, Calendar.MINUTE, Calendar.HOUR_OF_DAY);
  }

  /**
   * Floors the given fields in the given calendar instance, that is, sets them to zero, if no fields are specified
   * the calendar is left unmodified
   * @param calendar the calendar in which to floor fields
   * @param fields the fields to floor
   */
  public static void floorFields(final Calendar calendar, final Integer... fields) {
    Util.rejectNullValue(calendar, "calendar");
    if (fields != null) {
      for (final Integer field : fields) {
        calendar.set(field, 0);
      }
    }
  }

  /**
   * @param year the year
   * @param month the month, note this is zero based, see {@link Calendar#MONTH}
   * @param day the day
   * @return a Date based on the given values
   */
  public static Date getDate(final int year, final int month, final int day) {
    final Calendar calendar = Calendar.getInstance();
    floorTimeFields(calendar);
    calendar.set(Calendar.YEAR, year);
    calendar.set(Calendar.MONTH, month);
    calendar.set(Calendar.DAY_OF_MONTH, day);

    return calendar.getTime();
  }

  /**
   * @return yesterday
   */
  public static Date getYesterday() {
    final Calendar calendar = Calendar.getInstance();
    floorTimeFields(calendar);
    calendar.add(Calendar.DAY_OF_MONTH, -1);

    return calendar.getTime();
  }

  /**
   * @return the first day of last month
   */
  public static Date getFirstDayOfLastMonth() {
    return getFirstDayOfMonth(-1);
  }

  /**
   * @return the last day of last month
   */
  public static Date getLastDayOfLastMonth() {
    return getLastDayOfMonth(-1);
  }

  /**
   * @param toAdd the number of months to add to the current month
   * @return the first day of the month <code>toAdd</code> from the current month
   */
  public static Date getFirstDayOfMonth(final int toAdd) {
    final Calendar calendar = Calendar.getInstance();
    floorTimeFields(calendar);
    calendar.add(Calendar.MONTH, toAdd);
    calendar.set(Calendar.DAY_OF_MONTH, 1);

    return calendar.getTime();
  }

  /**
   * @param toAdd the number of months to add to the current month
   * @return the last day of the month <code>toAdd</code> from the current month
   */
  public static Date getLastDayOfMonth(final int toAdd) {
    final Calendar calendar = Calendar.getInstance();
    floorTimeFields(calendar);
    calendar.add(Calendar.MONTH, toAdd);
    calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));

    return calendar.getTime();
  }

  /**
   * @return the first day of the current year
   */
  public static Date getFirstDayOfYear() {
    final Calendar calendar = Calendar.getInstance();
    floorTimeFields(calendar);
    calendar.set(Calendar.MONTH, Calendar.JANUARY);
    calendar.set(Calendar.DAY_OF_MONTH, 1);

    return calendar.getTime();
  }

  /**
   * @return the last day of the current year
   */
  public static Date getLastDayOfYear() {
    final Calendar calendar = Calendar.getInstance();
    floorTimeFields(calendar);
    calendar.set(Calendar.MONTH, Calendar.DECEMBER);
    calendar.set(Calendar.DAY_OF_MONTH, THIRTY_FIRST);

    return calendar.getTime();
  }

  /**
   * Returns the first day of the the given quarter, assuming quarters begin
   * in january, april, july and october.
   * @param quarter the Quarter
   * @return the first day of the given quarter
   */
  public static Date getFirstDayOfQuarter(final Quarter quarter) {
    final Calendar calendar = Calendar.getInstance();
    floorTimeFields(calendar);
    calendar.set(Calendar.DAY_OF_MONTH, 1);
    switch (quarter) {
      case FIRST: {
        calendar.set(Calendar.MONTH, Calendar.JANUARY);
        return calendar.getTime();
      }
      case SECOND: {
        calendar.set(Calendar.MONTH, Calendar.APRIL);
        return calendar.getTime();
      }
      case THIRD: {
        calendar.set(Calendar.MONTH, Calendar.JULY);
        return calendar.getTime();
      }
      case FOURTH: {
        calendar.set(Calendar.MONTH, Calendar.OCTOBER);
        return calendar.getTime();
      }
    }

    return null;
  }

  /**
   * Returns the last day of the the given quarter, assuming quarters begin
   * in january, april, july and october.
   * @param quarter the Quarter
   * @return the last day of the given quarter
   */
  public static Date getLastDayOfQuarter(final Quarter quarter) {
    final Calendar calendar = Calendar.getInstance();
    floorTimeFields(calendar);
    switch (quarter) {
      case FIRST: {
        calendar.set(Calendar.MONTH, Calendar.MARCH);
        calendar.set(Calendar.DAY_OF_MONTH, THIRTY_FIRST);
        return calendar.getTime();
      }
      case SECOND: {
        calendar.set(Calendar.MONTH, Calendar.JUNE);
        calendar.set(Calendar.DAY_OF_MONTH, THIRTIETH);
        return calendar.getTime();
      }
      case THIRD: {
        calendar.set(Calendar.MONTH, Calendar.SEPTEMBER);
        calendar.set(Calendar.DAY_OF_MONTH, THIRTIETH);
        return calendar.getTime();
      }
      case FOURTH: {
        calendar.set(Calendar.MONTH, Calendar.DECEMBER);
        calendar.set(Calendar.DAY_OF_MONTH, THIRTY_FIRST);
        return calendar.getTime();
      }
    }

    return null;
  }

  /**
   * @param from the from date, inclusive
   * @param to the to date, inclusive
   * @return the number of days in the given interval, including the from and to dates
   */
  public static int numberOfDaysInRange(final Date from, final Date to) {
    Util.rejectNullValue(from, "from");
    Util.rejectNullValue(to, "to");
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

  /**
   * @param formatString the format string
   * @return a thread local date format based on the given format string
   */
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
   * @param dateFormat the SimpleDateFormat for which to retrieve the date mask
   * @return a String representing the mask to use in JFormattedTextFields, i.e. "##-##-####"
   */
  public static String getDateMask(final SimpleDateFormat dateFormat) {
    Util.rejectNullValue(dateFormat, "dateFormat");
    final String datePattern = dateFormat.toPattern();
    final StringBuilder stringBuilder = new StringBuilder(datePattern.length());
    for (final Character character : datePattern.toCharArray()) {
      stringBuilder.append(Character.isLetter(character) ? "#" : character);
    }

    return stringBuilder.toString();
  }
}
