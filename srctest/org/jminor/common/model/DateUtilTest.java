/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.jminor.common.model.formats.DateFormats;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * User: Björn Darri
 * Date: 3.8.2009
 * Time: 00:14:32
 */
public class DateUtilTest {

  @Test
  public void isDateValid() throws Exception {
    assertTrue("isDateValid should work", DateUtil.isDateValid("03-10-1975", DateFormats.getDateFormat(DateFormats.SHORT_DASH)));
    assertFalse("isDateValid should work with an invalid date", DateUtil.isDateValid("033-102-975", DateFormats.getDateFormat(DateFormats.SHORT_DASH)));

    assertTrue("isDateValid should work with an empty string", DateUtil.isDateValid("", true, DateFormats.getDateFormat(DateFormats.SHORT_DASH)));

    assertTrue("isDateValid should work with long date", DateUtil.isDateValid("03-10-1975 10:45", false, DateFormats.getDateFormat(DateFormats.TIMESTAMP)));

    assertTrue("isDateValid should work with a date format specified",
            DateUtil.isDateValid("03.10.1975", false, DateFormats.getDateFormat(DateFormats.SHORT_DOT)));
  }
}
