/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;

/**
 * A static utility class for date handling.
 */
public final class DateUtil {

  /**
   * The quarters of the year
   */
  public enum Quarter {
    FIRST, SECOND, THIRD, FOURTH
  }

  private static final int THIRTY_FIRST = 31;
  private static final int THIRTIETH = 30;

  private DateUtil() {}

  /**
   * @param date the Date object to floor
   * @return a Date object with the same date as {@code date}, except the Calendar.HOUR_OF_DAY,
   * Calendar.MINUTE, Calendar.SECOND and Calendar.MILLISECOND fields are set to zero
   */
  public static Date floorDate(final Date date) {
    final Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    floorFields(cal, Arrays.asList(Calendar.MILLISECOND, Calendar.SECOND, Calendar.MINUTE, Calendar.HOUR_OF_DAY));

    return cal.getTime();
  }

  /**
   * Floors the time fields in the given calendar, Calendar.HOUR_OF_DAY,
   * Calendar.MINUTE, Calendar.SECOND and Calendar.MILLISECOND
   * @param calendar the calendar in which to floor the time fields
   */
  public static void floorTimeFields(final Calendar calendar) {
    floorFields(calendar, Arrays.asList(Calendar.MILLISECOND, Calendar.SECOND, Calendar.MINUTE, Calendar.HOUR_OF_DAY));
  }

  /**
   * Floors the given fields in the given calendar instance, that is, sets them to zero, if no fields are specified
   * the calendar is left unmodified
   * @param calendar the calendar in which to floor fields
   * @param fields the fields to floor
   */
  public static void floorFields(final Calendar calendar, final Collection<Integer> fields) {
    Objects.requireNonNull(calendar, "calendar");
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
   * @return the first day of the month {@code toAdd} from the current month
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
   * @return the last day of the month {@code toAdd} from the current month
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
  public static Date getFirstDayOfCurrentYear() {
    return getFirstDayOfYear(Calendar.getInstance().get(Calendar.YEAR));
  }

  /**
   * @param year the year
   * @return the first day of the given year
   */
  public static Date getFirstDayOfYear(final int year) {
    final Calendar calendar = Calendar.getInstance();
    floorTimeFields(calendar);
    calendar.set(Calendar.YEAR, year);
    calendar.set(Calendar.MONTH, Calendar.JANUARY);
    calendar.set(Calendar.DAY_OF_MONTH, 1);

    return calendar.getTime();
  }

  /**
   * @return the last day of the current year
   */
  public static Date getLastDayOfCurrentYear() {
    return getLastDayOfYear(Calendar.getInstance().get(Calendar.YEAR));
  }

  /**
   * @param year the year
   * @return the last day of the given year
   */
  public static Date getLastDayOfYear(final int year) {
    final Calendar calendar = Calendar.getInstance();
    floorTimeFields(calendar);
    calendar.set(Calendar.YEAR, year);
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
      case FIRST:
        calendar.set(Calendar.MONTH, Calendar.JANUARY);
        return calendar.getTime();
      case SECOND:
        calendar.set(Calendar.MONTH, Calendar.APRIL);
        return calendar.getTime();
      case THIRD:
        calendar.set(Calendar.MONTH, Calendar.JULY);
        return calendar.getTime();
      case FOURTH:
        calendar.set(Calendar.MONTH, Calendar.OCTOBER);
        return calendar.getTime();
      default:
        throw new IllegalArgumentException("Not a Quarter: " + quarter);
    }
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
      case FIRST:
        calendar.set(Calendar.MONTH, Calendar.MARCH);
        calendar.set(Calendar.DAY_OF_MONTH, THIRTY_FIRST);
        return calendar.getTime();
      case SECOND:
        calendar.set(Calendar.MONTH, Calendar.JUNE);
        calendar.set(Calendar.DAY_OF_MONTH, THIRTIETH);
        return calendar.getTime();
      case THIRD:
        calendar.set(Calendar.MONTH, Calendar.SEPTEMBER);
        calendar.set(Calendar.DAY_OF_MONTH, THIRTIETH);
        return calendar.getTime();
      case FOURTH:
        calendar.set(Calendar.MONTH, Calendar.DECEMBER);
        calendar.set(Calendar.DAY_OF_MONTH, THIRTY_FIRST);
        return calendar.getTime();
      default:
        throw new IllegalArgumentException("Not a Quarter: " + quarter);
    }
  }

  /**
   * @param from the from date, inclusive
   * @param to the to date, inclusive
   * @return the number of days in the given interval, including the from and to dates
   */
  public static int numberOfDaysInRange(final Date from, final Date to) {
    Objects.requireNonNull(from, "from");
    Objects.requireNonNull(to, "to");
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
}
