/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.junit.Test;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertEquals;

public class TimeUtilTest {

  @Test
  public void getTime() throws ParseException {
    final Date dateWithTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse("1975-10-03 10:45:42.123");
    final Date date = TimeUtil.getTime(dateWithTime);
    final Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    assertEquals(1970, calendar.get(Calendar.YEAR));
    assertEquals(Calendar.JANUARY, calendar.get(Calendar.MONTH));
    assertEquals(1, calendar.get(Calendar.DAY_OF_MONTH));
    assertEquals(10, calendar.get(Calendar.HOUR_OF_DAY));
    assertEquals(45, calendar.get(Calendar.MINUTE));
    assertEquals(42, calendar.get(Calendar.SECOND));
    assertEquals(123, calendar.get(Calendar.MILLISECOND));
  }

  @Test
  public void floorTimestamp() {
    final Timestamp timestamp = TimeUtil.floorTimestamp(new Timestamp(System.currentTimeMillis()));
    final Calendar calendar = Calendar.getInstance();
    calendar.setTime(timestamp);
    assertEquals(0, calendar.get(Calendar.MILLISECOND));
    assertEquals(0, calendar.get(Calendar.SECOND));
  }
}
