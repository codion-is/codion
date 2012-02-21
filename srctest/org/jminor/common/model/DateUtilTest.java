/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.jminor.common.model.formats.DateFormats;

import org.junit.Test;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.*;

public class DateUtilTest {

  @Test
  public void isDateValid() throws Exception {
    assertTrue("isDateValid should work", DateUtil.isDateValid("03-10-1975", DateFormats.getDateFormat(DateFormats.SHORT_DASH)));
    assertFalse("isDateValid should work with an invalid date", DateUtil.isDateValid("033-102-975", DateFormats.getDateFormat(DateFormats.SHORT_DASH)));

    assertTrue("isDateValid should work with an empty string", DateUtil.isDateValid("", true, DateFormats.getDateFormat(DateFormats.SHORT_DASH)));

    assertTrue("isDateValid should work with long date", DateUtil.isDateValid("03-10-1975 10:45", false, DateFormats.getDateFormat(DateFormats.TIMESTAMP)));

    assertTrue("isDateValid should work with a date format specified",
            DateUtil.isDateValid("03.10.1975", false, DateFormats.getDateFormat(DateFormats.SHORT_DOT)));
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void isDateValidNullFormat() {
    DateUtil.isDateValid("03-10-1975");
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void floorFieldsNullCalendar() {
    DateUtil.floorFields(null);
  }
  
  @Test
  public void getDate() throws ParseException {
    final Date date = DateUtil.getDate(1975, Calendar.OCTOBER, 3);
    assertEquals(DateFormats.getDateFormat(DateFormats.SHORT_DASH).parse("03-10-1975"), date);
  }
  
  @Test
  public void floorTimeFields() throws ParseException {
    final Date dateWithTime = DateFormats.getDateFormat(DateFormats.EXACT_TIMESTAMP).parse("1975-10-03 10:45:42.123");
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
  public void floorTimestamp() {
    final Timestamp timestamp = DateUtil.floorTimestamp(new Timestamp(System.currentTimeMillis()));
    final Calendar calendar = Calendar.getInstance();
    calendar.setTime(timestamp);
    assertEquals(0, calendar.get(Calendar.MILLISECOND));
    assertEquals(0, calendar.get(Calendar.SECOND));
  }
}
