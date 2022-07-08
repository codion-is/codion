/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.formats;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class LocaleDateTimePatternTest {

  @Test
  void locale() {
    LocaleDateTimePattern pattern = LocaleDateTimePattern.builder()
            .delimiterDash()
            .yearFourDigits()
            .hoursMinutes()
            .build();

    Locale iceland = new Locale("is", "IS");
    Locale us = new Locale("en", "US");

    assertEquals("HH:mm", pattern.getTimePattern());
    assertEquals("dd-MM-yyyy", pattern.getDatePattern(iceland));
    assertEquals("MM-dd-yyyy", pattern.getDatePattern(us));
    assertEquals("dd-MM-yyyy HH:mm", pattern.getDateTimePattern(iceland));
    assertEquals("MM-dd-yyyy HH:mm", pattern.getDateTimePattern(us));

    pattern = LocaleDateTimePattern.builder()
            .delimiterDot()
            .yearTwoDigits()
            .hoursMinutesSeconds()
            .build();
    assertEquals("HH:mm:ss", pattern.getTimePattern());
    assertEquals("dd.MM.yy", pattern.getDatePattern(iceland));
    assertEquals("MM.dd.yy", pattern.getDatePattern(us));
    assertEquals("dd.MM.yy HH:mm:ss", pattern.getDateTimePattern(iceland));
    assertEquals("MM.dd.yy HH:mm:ss", pattern.getDateTimePattern(us));

    pattern = LocaleDateTimePattern.builder()
            .delimiterSlash()
            .yearFourDigits()
            .hoursMinutesSecondsMilliseconds()
            .build();

    assertEquals("HH:mm:ss.SSS", pattern.getTimePattern());
    assertEquals("dd/MM/yyyy", pattern.getDatePattern(iceland));
    assertEquals("MM/dd/yyyy", pattern.getDatePattern(us));
    assertEquals("dd/MM/yyyy HH:mm:ss.SSS", pattern.getDateTimePattern(iceland));
    assertEquals("MM/dd/yyyy HH:mm:ss.SSS", pattern.getDateTimePattern(us));

    //a bit of coverage
    assertNotNull(pattern.createFormatter());
    pattern.getDatePattern();
    pattern.getDateTimePattern();
  }
}
