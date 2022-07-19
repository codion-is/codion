/*
 * Copyright (c) 2016 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.formats;

import org.junit.jupiter.api.Test;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class FormatsTest {

  @Test
  void test() throws ParseException {
    NumberFormat format = Formats.getNonGroupingNumberFormat();
    assertFalse(format.isGroupingUsed());
    format = Formats.getNonGroupingIntegerFormat();
    assertFalse(format.isGroupingUsed());
    assertTrue(format.isParseIntegerOnly());
    format = Formats.getBigDecimalNumberFormat();
    assertTrue(format instanceof DecimalFormat);
    assertTrue(((DecimalFormat) format).isParseBigDecimal());
  }
}
