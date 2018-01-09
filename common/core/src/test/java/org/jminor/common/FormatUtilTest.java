/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import org.junit.Test;

import java.text.NumberFormat;
import java.text.ParseException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public final class FormatUtilTest {

  @Test
  public void test() throws ParseException {
    assertEquals("test123", FormatUtil.NULL_FORMAT.format("test123"));
    assertEquals("test123", FormatUtil.NULL_FORMAT.parseObject("test123"));
    final NumberFormat format = FormatUtil.getNonGroupingNumberFormat();
    assertFalse(format.isGroupingUsed());
  }
}
