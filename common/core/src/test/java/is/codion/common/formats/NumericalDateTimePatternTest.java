/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.formats;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class NumericalDateTimePatternTest {

  @Test
  public void getDateMask() {
    assertEquals("##-##-####", NumericalDateTimePattern.getMask("dd-MM-yyyy"));
  }

  @Test
  public void locale() {
    final NumericalDateTimePattern dateFormat =
            NumericalDateTimePattern.builder().delimiter("-").fourDigitYear().hoursMinutes().build();

    final Locale iceland = new Locale("is", "IS");
    final Locale us = new Locale("en", "US");

    assertEquals("dd-MM-yyyy", dateFormat.getDatePattern(iceland));
    assertEquals("MM-dd-yyyy", dateFormat.getDatePattern(us));
    assertEquals("dd-MM-yyyy HH:mm", dateFormat.getDateTimePattern(iceland));
    assertEquals("MM-dd-yyyy HH:mm", dateFormat.getDateTimePattern(us));
  }
}
