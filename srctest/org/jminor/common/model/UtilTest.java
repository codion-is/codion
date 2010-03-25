/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import static org.junit.Assert.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class UtilTest {

  @Test
  public void roundDouble() {
    final Double d = 5.1234567;
    assertEquals(new Double(5.1), new Double(Util.roundDouble(d, 1)));
    assertEquals(new Double(5.12), new Double(Util.roundDouble(d, 2)));
    assertEquals(new Double(5.123), new Double(Util.roundDouble(d, 3)));
    assertEquals(new Double(5.1235), new Double(Util.roundDouble(d, 4)));
    assertEquals(new Double(5.12346), new Double(Util.roundDouble(d, 5)));
    assertEquals(new Double(5.123457), new Double(Util.roundDouble(d, 6)));
    assertEquals(new Double(5.1234567), new Double(Util.roundDouble(d, 7)));
  }

  @Test
  public void padString() {
    final String string = "hello";
    assertEquals("hello", Util.padString(string, 4, '*', true));
    assertEquals("hello", Util.padString(string, 5, '*', true));
    assertEquals("***hello", Util.padString(string, 8, '*', true));
    assertEquals("hello***", Util.padString(string, 8, '*', false));
  }

  @Test
  public void equal() throws Exception {
    assertTrue("Two null values should be equal", Util.equal(null, null));
  }

  @Test
  public void getArrayContentsAsString() throws Exception {
    final String res = Util.getArrayContentsAsString(new Integer[] {1,2,3,4}, false);
    assertEquals("Integer array as string should work", "1, 2, 3, 4", res);
  }

  @Test
  public void getDouble() throws Exception {
    assertEquals("getDouble should work with comma", new Double(4.22), Util.getDouble("4,22"));
    assertEquals("getDouble should work with period", new Double(4.22), Util.getDouble("4.22"));
    assertEquals("getDouble should work with single minus sign", new Double(-1), Util.getDouble("-"));
    assertNull("getDouble should work with an empty string", Util.getDouble(""));
  }

  @Test
  public void getInt() throws Exception {
    assertEquals("getInt should work with a digit string", new Integer(4), Util.getInt("4"));
    assertEquals("getInt should work with single minus sign", new Integer(-1), Util.getInt("-"));
    assertNull("getInt should work with an empty string", Util.getInt(""));
  }

  @Test
  public void getListContentsAsString() throws Exception {
    final List<Integer> list = new ArrayList<Integer>();
    list.add(1);
    list.add(2);
    list.add(3);
    list.add(4);
    final String res = Util.getListContentsAsString(list, false);
    assertEquals("Integer list as string should work", "1, 2, 3, 4", res);
  }

  @Test
  public void getLong() throws Exception {
    assertEquals("getLong should work with a digit string", new Long(4), Util.getLong("4"));
    assertEquals("getLong should work with single minus sign", new Long(-1), Util.getLong("-"));
    assertNull("getLong should work with an empty string", Util.getLong(""));
  }

  @Test
  public void notNull() throws Exception {
    assertTrue(Util.notNull(new Object(), new Object(), new Object()));
    assertTrue(Util.notNull(new Object()));
    assertFalse(Util.notNull(new Object(), null, new Object()));
    Object ob = null;
    assertFalse(Util.notNull(ob));
  }
}
