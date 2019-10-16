/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Objects.requireNonNull;

/**
 * A utility class for date handling.
 */
public final class DateUtil {

  /**
   * The quarters of the year
   */
  public enum Quarter {
    FIRST, SECOND, THIRD, FOURTH
  }

  private static final int THIRTY_ONE = 31;
  private static final int THIRTY = 30;

  private DateUtil() {}

  /**
   * @return yesterday
   */
  public static LocalDate getYesterday() {
    return LocalDate.now().minus(1, DAYS);
  }

  /**
   * @return the first day of last month
   */
  public static LocalDate getFirstDayOfLastMonth() {
    return getFirstDayOfMonth(-1);
  }

  /**
   * @return the last day of last month
   */
  public static LocalDate getLastDayOfLastMonth() {
    return getLastDayOfMonth(-1);
  }

  /**
   * @param toAdd the number of months to add to the current month
   * @return the first day of the month {@code toAdd} from the current month
   */
  public static LocalDate getFirstDayOfMonth(final int toAdd) {
    return LocalDate.now().plus(toAdd, ChronoUnit.MONTHS).withDayOfMonth(1);
  }

  /**
   * @param toAdd the number of months to add to the current month
   * @return the last day of the month {@code toAdd} from the current month
   */
  public static LocalDate getLastDayOfMonth(final int toAdd) {
    final LocalDate now = LocalDate.now();

    return now.plus(toAdd, ChronoUnit.MONTHS).withDayOfMonth(YearMonth.of(now.getYear(), now.getMonth()).lengthOfMonth());
  }

  /**
   * @return the first day of the current year
   */
  public static LocalDate getFirstDayOfCurrentYear() {
    return getFirstDayOfYear(LocalDate.now().getYear());
  }

  /**
   * @param year the year
   * @return the first day of the given year
   */
  public static LocalDate getFirstDayOfYear(final int year) {
    return LocalDate.of(year, Month.JANUARY.getValue(), 1);
  }

  /**
   * @return the last day of the current year
   */
  public static LocalDate getLastDayOfCurrentYear() {
    return getLastDayOfYear(LocalDate.now().getYear());
  }

  /**
   * @param year the year
   * @return the last day of the given year
   */
  public static LocalDate getLastDayOfYear(final int year) {
    return LocalDate.now().withYear(year).withMonth(Month.DECEMBER.getValue()).withDayOfMonth(THIRTY_ONE);
  }

  /**
   * Returns the first day of the the given quarter, assuming quarters begin
   * in january, april, july and october.
   * @param quarter the Quarter
   * @return the first day of the given quarter
   */
  public static LocalDate getFirstDayOfQuarter(final Quarter quarter) {
    switch (quarter) {
      case FIRST:
        return LocalDate.now().withMonth(Month.JANUARY.getValue()).withDayOfMonth(1);
      case SECOND:
        return LocalDate.now().withMonth(Month.APRIL.getValue()).withDayOfMonth(1);
      case THIRD:
        return LocalDate.now().withMonth(Month.JULY.getValue()).withDayOfMonth(1);
      case FOURTH:
        return LocalDate.now().withMonth(Month.OCTOBER.getValue()).withDayOfMonth(1);
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
  public static LocalDate getLastDayOfQuarter(final Quarter quarter) {
    final LocalDate now = LocalDate.now();
    switch (quarter) {
      case FIRST:
        return now.withMonth(Month.MARCH.getValue()).withDayOfMonth(THIRTY_ONE);
      case SECOND:
        return now.withMonth(Month.JUNE.getValue()).withDayOfMonth(THIRTY);
      case THIRD:
        return now.withMonth(Month.SEPTEMBER.getValue()).withDayOfMonth(THIRTY);
      case FOURTH:
        return now.withMonth(Month.DECEMBER.getValue()).withDayOfMonth(THIRTY_ONE);
      default:
        throw new IllegalArgumentException("Not a Quarter: " + quarter);
    }
  }

  /**
   * Calculates the number of days between the two dates, note that this is inclusive so
   * the number of days between two instances of the same date is 1.
   * @param from the from date, inclusive
   * @param to the to date, inclusive
   * @return the number of days in the given interval, including the from and to dates
   */
  public static long numberOfDaysInRange(final LocalDate from, final LocalDate to) {
    if (requireNonNull(from, "from").isAfter(requireNonNull(to, "to"))) {
      throw new IllegalArgumentException("'To' date should be after 'from' date");
    }

    return DAYS.between(from, to) + 1;
  }
}
