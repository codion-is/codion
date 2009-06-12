package org.jminor.common.model;

import org.jminor.common.model.formats.ShortDotDateFormat;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

public class TestUtil extends TestCase {

  public TestUtil() {
    super("TestUtil");
  }

  public void testEqual() throws Exception {
    assertTrue("Two null values should be equal", Util.equal(null, null));
  }

  public void testGetArrayContentsAsString() throws Exception {
    final String res = Util.getArrayContentsAsString(new Integer[] {1,2,3,4}, false);
    assertEquals("Integer array as string should work", "1, 2, 3, 4", res);
  }

  public void testGetDouble() throws Exception {
    assertEquals("getDouble should work with comma", 4.22, Util.getDouble("4,22"));
    assertEquals("getDouble should work with period", 4.22, Util.getDouble("4.22"));
    assertEquals("getDouble should work with single minus sign", -1d, Util.getDouble("-"));
    assertNull("getDouble should work with an empty string", Util.getDouble(""));
  }

  public void testGetInt() throws Exception {
    assertEquals("getInt should work with a digit string", new Integer(4), Util.getInt("4"));
    assertEquals("getInt should work with single minus sign", new Integer(-1), Util.getInt("-"));
    assertNull("getInt should work with an empty string", Util.getInt(""));
  }

  public void testGetListContentsAsString() throws Exception {
    final List<Integer> list = new ArrayList<Integer>();
    list.add(1);
    list.add(2);
    list.add(3);
    list.add(4);
    final String res = Util.getListContentsAsString(list, false);
    assertEquals("Integer list as string should work", "1, 2, 3, 4", res);
  }

  public void testGetLong() throws Exception {
    assertEquals("getLong should work with a digit string", new Long(4), Util.getLong("4"));
    assertEquals("getLong should work with single minus sign", new Long(-1), Util.getLong("-"));
    assertNull("getLong should work with an empty string", Util.getLong(""));
  }

  public void testIsDateOk() throws Exception {
    assertTrue("isDateValid should work", Util.isDateValid("03-10-1975"));
    assertFalse("isDateValid should work with an invalid date", Util.isDateValid("033-102-975"));

    assertTrue("isDateValid should work with an empty string", Util.isDateValid("", true));

    assertTrue("isDateValid should work with long date", Util.isDateValid("03-10-1975 10:45", false, true));

    assertTrue("isDateValid should work with a date format specified", Util.isDateValid("03.10.1975", false, new ShortDotDateFormat()));
  }
}
