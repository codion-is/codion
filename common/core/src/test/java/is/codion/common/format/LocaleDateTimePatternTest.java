/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson.
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

    assertEquals("HH:mm", pattern.timePattern());
    assertEquals("dd-MM-yyyy", pattern.datePattern(iceland));
    assertEquals("MM-dd-yyyy", pattern.datePattern(us));
    assertEquals("dd-MM-yyyy HH:mm", pattern.dateTimePattern(iceland));
    assertEquals("MM-dd-yyyy HH:mm", pattern.dateTimePattern(us));

    pattern = LocaleDateTimePattern.builder()
            .delimiterDot()
            .yearTwoDigits()
            .hoursMinutesSeconds()
            .build();
    assertEquals("HH:mm:ss", pattern.timePattern());
    assertEquals("dd.MM.yy", pattern.datePattern(iceland));
    assertEquals("MM.dd.yy", pattern.datePattern(us));
    assertEquals("dd.MM.yy HH:mm:ss", pattern.dateTimePattern(iceland));
    assertEquals("MM.dd.yy HH:mm:ss", pattern.dateTimePattern(us));

    pattern = LocaleDateTimePattern.builder()
            .delimiterSlash()
            .yearFourDigits()
            .hoursMinutesSecondsMilliseconds()
            .build();

    assertEquals("HH:mm:ss.SSS", pattern.timePattern());
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
