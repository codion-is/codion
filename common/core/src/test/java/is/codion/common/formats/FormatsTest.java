/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.formats;

import org.junit.jupiter.api.Test;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;

import static org.junit.jupiter.api.Assertions.*;

public final class FormatsTest {

  @Test
  void test() throws ParseException {
    assertEquals("test123", Formats.NULL_FORMAT.format("test123"));
    assertEquals("test123", Formats.NULL_FORMAT.parseObject("test123"));
    NumberFormat format = Formats.getNonGroupingNumberFormat();
    assertFalse(format.isGroupingUsed());
    format = Formats.getNonGroupingIntegerFormat();
    assertFalse(format.isGroupingUsed());
    assertTrue(format.isParseIntegerOnly());
    format = Formats.getBigDecimalNumberFormat();
    assertTrue(format instanceof DecimalFormat);
    assertTrue(((DecimalFormat) format).isParseBigDecimal());
    assertEquals("test", Formats.NULL_FORMAT.format("test"));
    assertEquals("", Formats.NULL_FORMAT.format(null));
  }
}
