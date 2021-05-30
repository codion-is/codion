/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.formats;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class LocaleDateTimePatternTest {

  @Test
  void getDateMask() {
    assertEquals("##-##-####", LocaleDateTimePattern.getMask("dd-MM-yyyy"));
  }

  @Test
  void locale() {
    final LocaleDateTimePattern pattern = LocaleDateTimePattern.builder()
            .delimiterDash().yearFourDigits().hoursMinutes()
            .build();

    final Locale iceland = new Locale("is", "IS");
    final Locale us = new Locale("en", "US");

    assertEquals("dd-MM-yyyy", pattern.getDatePattern(iceland));
    assertEquals("MM-dd-yyyy", pattern.getDatePattern(us));
    assertEquals("dd-MM-yyyy HH:mm", pattern.getDateTimePattern(iceland));
    assertEquals("MM-dd-yyyy HH:mm", pattern.getDateTimePattern(us));
  }
}
