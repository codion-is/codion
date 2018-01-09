/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import org.junit.Test;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.*;

public class DateUtilTest {

  private static final SimpleDateFormat SHORT_DASH = new SimpleDateFormat("dd-MM-yyyy");
  private static final SimpleDateFormat SHORT_DOT = new SimpleDateFormat("dd.MM.yyyy");
  private static final SimpleDateFormat TIMESTAMP = new SimpleDateFormat("dd-MM-yyyy HH:mm");
  private static final SimpleDateFormat EXACT_TIMESTAMP = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

  @Test
  public void isDateValid() throws Exception {
    assertTrue("isDateValid should work", DateUtil.isDateValid("03-10-1975", SHORT_DASH));
    assertFalse("isDateValid should work with an invalid date", DateUtil.isDateValid("033-102-975", SHORT_DASH));

    assertTrue("isDateValid should work with an empty string", DateUtil.isDateValid("", true, SHORT_DASH));
    assertFalse("isDateValid should not work with an empty string", DateUtil.isDateValid("", false, SHORT_DASH));

    assertTrue("isDateValid should work with long date", DateUtil.isDateValid("03-10-1975 10:45", false, TIMESTAMP));

    assertTrue("isDateValid should work with a date format specified", DateUtil.isDateValid("03.10.1975", false, SHORT_DOT));
  }

  @Test(expected = NullPointerException.class)
  public void floorFieldsNullCalendar() {
    DateUtil.floorFields(null, Collections.emptyList());
  }

  @Test
  public void getDate() throws ParseException {
    final Date date = DateUtil.getDate(1975, Calendar.OCTOBER, 3);
    assertEquals(SHORT_DASH.parse("03-10-1975"), date);
  }

  @Test
  public void floorTimeFields() throws ParseException {
    final Date dateWithTime = EXACT_TIMESTAMP.parse("1975-10-03 10:45:42.123");
    final Calendar calendar = Calendar.getInstance();
    calendar.setTime(dateWithTime);
    DateUtil.floorTimeFields(calendar);
    assertEquals(0, calendar.get(Calendar.MILLISECOND));
    assertEquals(0, calendar.get(Calendar.SECOND));
    assertEquals(0, calendar.get(Calendar.MINUTE));
    assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY));
  }

  @Test
  public void floorDate() {
    final Date date = DateUtil.floorDate(new Date());
    final Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    assertEquals(0, calendar.get(Calendar.MILLISECOND));
    assertEquals(0, calendar.get(Calendar.SECOND));
    assertEquals(0, calendar.get(Calendar.MINUTE));
    assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY));
  }

  @Test
  public void floorFields() throws ParseException {
    final Date dateWithTime = EXACT_TIMESTAMP.parse("1975-10-03 10:45:42.123");
    final Calendar calendar = Calendar.getInstance();
    calendar.setTime(dateWithTime);
    DateUtil.floorFields(calendar, Arrays.asList(Calendar.MINUTE, Calendar.MILLISECOND));
    assertEquals(1975, calendar.get(Calendar.YEAR));
    assertEquals(Calendar.OCTOBER, calendar.get(Calendar.MONTH));
    assertEquals(3, calendar.get(Calendar.DAY_OF_MONTH));
    assertEquals(10, calendar.get(Calendar.HOUR_OF_DAY));
    assertEquals(0, calendar.get(Calendar.MINUTE));
    assertEquals(42, calendar.get(Calendar.SECOND));
    assertEquals(0, calendar.get(Calendar.MILLISECOND));
    DateUtil.floorFields(calendar, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void isDateValidMissingDateFormat() {
    DateUtil.isDateValid("03-10-1975", false);
  }

  @Test(expected = IllegalArgumentException.class)
  public void isDateValidNullDateFormat() {
    DateUtil.isDateValid("03-10-1975", false, (DateFormat[]) null);
  }

  @Test
  public void numberOfDaysInRange() {
    final Date start = DateUtil.getDate(2011, Calendar.JANUARY, 1);
    Date end = DateUtil.getDate(2011, Calendar.JANUARY, 1);
    assertEquals(1, DateUtil.numberOfDaysInRange(start, end));

    end = DateUtil.getDate(2011, Calendar.JANUARY, 2);
    assertEquals(2, DateUtil.numberOfDaysInRange(start, end));
    end = DateUtil.getDate(2011, Calendar.JANUARY, 3);
    assertEquals(3, DateUtil.numberOfDaysInRange(start, end));
    end = DateUtil.getDate(2011, Calendar.FEBRUARY, 1);
    assertEquals(32, DateUtil.numberOfDaysInRange(start, end));
  }

  @Test(expected = IllegalArgumentException.class)
  public void numberOfDaysInRangeToAfterFrom() {
    assertEquals(1, DateUtil.numberOfDaysInRange(DateUtil.getDate(2011, Calendar.FEBRUARY, 1), DateUtil.getDate(2011, Calendar.JANUARY, 1)));
  }

  @Test
  public void getDateMask() {
    assertEquals("##-##-####", DateUtil.getDateMask(new SimpleDateFormat("dd-MM-yyyy")));
  }
}
