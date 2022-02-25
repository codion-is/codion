/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.formats;

import org.junit.jupiter.api.Test;

import java.text.NumberFormat;
import java.text.ParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public final class FormatsTest {

  @Test
  void test() throws ParseException {
    assertEquals("test123", Formats.NULL_FORMAT.format("test123"));
    assertEquals("test123", Formats.NULL_FORMAT.parseObject("test123"));
    NumberFormat format = Formats.getNonGroupingNumberFormat();
    assertFalse(format.isGroupingUsed());
  }
}
