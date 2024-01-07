/*
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.format;

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

    assertEquals("HH:mm", pattern.timePattern().orElseThrow(IllegalStateException::new));
    assertEquals("dd-MM-yyyy", pattern.datePattern(iceland));
    assertEquals("MM-dd-yyyy", pattern.datePattern(us));
    assertEquals("dd-MM-yyyy HH:mm", pattern.dateTimePattern(iceland));
    assertEquals("MM-dd-yyyy HH:mm", pattern.dateTimePattern(us));

    pattern = LocaleDateTimePattern.builder()
            .delimiterDot()
            .yearTwoDigits()
            .hoursMinutesSeconds()
            .build();
    assertEquals("HH:mm:ss", pattern.timePattern().orElseThrow(IllegalStateException::new));
    assertEquals("dd.MM.yy", pattern.datePattern(iceland));
    assertEquals("MM.dd.yy", pattern.datePattern(us));
    assertEquals("dd.MM.yy HH:mm:ss", pattern.dateTimePattern(iceland));
    assertEquals("MM.dd.yy HH:mm:ss", pattern.dateTimePattern(us));

    pattern = LocaleDateTimePattern.builder()
            .delimiterSlash()
            .yearFourDigits()
            .hoursMinutesSecondsMilliseconds()
            .build();

    assertEquals("HH:mm:ss.SSS", pattern.timePattern().orElseThrow(IllegalStateException::new));
    assertEquals("dd/MM/yyyy", pattern.datePattern(iceland));
    assertEquals("MM/dd/yyyy", pattern.datePattern(us));
    assertEquals("dd/MM/yyyy HH:mm:ss.SSS", pattern.dateTimePattern(iceland));
    assertEquals("MM/dd/yyyy HH:mm:ss.SSS", pattern.dateTimePattern(us));

    //a bit of coverage
    assertNotNull(pattern.createFormatter());
    pattern.datePattern();
    pattern.dateTimePattern();
  }
}
