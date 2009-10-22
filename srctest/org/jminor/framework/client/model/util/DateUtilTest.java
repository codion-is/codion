package org.jminor.framework.client.model.util;

import org.jminor.common.model.formats.DateFormats;

import junit.framework.TestCase;

import java.text.SimpleDateFormat;

/**
 * User: Björn Darri
 * Date: 3.8.2009
 * Time: 00:14:32
 */
public class DateUtilTest extends TestCase {

  public void testIsDateOk() throws Exception {
    assertTrue("isDateValid should work", DateUtil.isDateValid("03-10-1975"));
    assertFalse("isDateValid should work with an invalid date", DateUtil.isDateValid("033-102-975"));

    assertTrue("isDateValid should work with an empty string", DateUtil.isDateValid("", true));

    assertTrue("isDateValid should work with long date", DateUtil.isDateValid("03-10-1975 10:45", false, true));

    assertTrue("isDateValid should work with a date format specified",
            DateUtil.isDateValid("03.10.1975", false, new SimpleDateFormat(DateFormats.SHORT_DOT)));
  }
}
