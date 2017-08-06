/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.DateUtil;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

/**
 * A static utility class for sql time and timestamp handling.
 */
public final class TimeUtil {

  private static final int NINETEENSEVENTY = 1970;

  private TimeUtil() {}

  /**
   * @param date the Date object to floor
   * @return a Time object with the same time of day as {@code date}, except the Calendar.YEAR,
   * Calendar.MONTH and Calendar.DATE fields are set to 1970, january and 1 respectively
   */
  public static Time getTime(final Date date) {
    final Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    cal.set(Calendar.YEAR, NINETEENSEVENTY);
    cal.set(Calendar.MONTH, Calendar.JANUARY);
    cal.set(Calendar.DATE, 1);

    return new Time(cal.getTimeInMillis());
  }

  /**
   * @param timestamp the Timestamp object to floor
   * @return a Timestamp object with the same time as {@code timestamp}
   * except the Calendar.SECOND and Calendar.MILLISECOND fields are set to zero
   */
  public static Timestamp floorTimestamp(final Timestamp timestamp) {
    final Calendar cal = Calendar.getInstance();
    cal.setTime(timestamp);
    DateUtil.floorFields(cal, Calendar.SECOND, Calendar.MILLISECOND);

    return new Timestamp(cal.getTimeInMillis());
  }
}
