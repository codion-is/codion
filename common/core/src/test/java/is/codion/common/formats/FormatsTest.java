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
    NumberFormat format = Formats.nonGroupingNumberFormat();
    assertFalse(format.isGroupingUsed());
    format = Formats.nonGroupingIntegerFormat();
    assertFalse(format.isGroupingUsed());
    assertTrue(format.isParseIntegerOnly());
    format = Formats.bigDecimalNumberFormat();
    assertTrue(format instanceof DecimalFormat);
    assertTrue(((DecimalFormat) format).isParseBigDecimal());
  }
}
