/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.jminor.common.Util;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.*;

public final class TextUtilTest {

  @Test
  public void spaceAwareCollator() {
    final String one = "björn";
    final String two = "bjö rn";
    final String three = "björ n";
    final List<String> strings = Arrays.asList(one, two, three);

    final Comparator<String> collator = TextUtil.getSpaceAwareCollator();

    Collections.sort(strings, collator);
    assertEquals(two, strings.get(0));
    assertEquals(three, strings.get(1));
    assertEquals(one, strings.get(2));
  }

  @Test(expected = IllegalArgumentException.class)
  public void createRandomStringMinLengthExceedsMaxLength() {
    TextUtil.createRandomString(3, 2);
  }

  @Test
  public void createRandomString() {
    String randomString = TextUtil.createRandomString(1, 1);
    assertEquals(1, randomString.length());
    randomString = TextUtil.createRandomString(5, 5);
    assertEquals(5, randomString.length());
    randomString = TextUtil.createRandomString(4, 10);
    assertTrue(randomString.length() >= 4);
    assertTrue(randomString.length() <= 10);
  }

  @Test
  public void padString() {
    final String string = "hello";
    assertEquals("hello", TextUtil.padString(string, 4, '*', true));
    assertEquals("hello", TextUtil.padString(string, 5, '*', true));
    assertEquals("***hello", TextUtil.padString(string, 8, '*', true));
    assertEquals("hello***", TextUtil.padString(string, 8, '*', false));
  }

  @Test
  public void getDouble() throws Exception {
    assertEquals("getDouble should work with comma", new Double(4.22), TextUtil.getDouble("4,22"));
    assertEquals("getDouble should work with period", new Double(4.22), TextUtil.getDouble("4.22"));
    assertEquals("getDouble should work with single minus sign", new Double(-1), TextUtil.getDouble("-"));
    assertNull("getDouble should work with an empty string", TextUtil.getDouble(""));
  }

  @Test
  public void getInt() throws Exception {
    assertEquals("getInt should work with a digit string", new Integer(4), TextUtil.getInt("4"));
    assertEquals("getInt should work with single minus sign", new Integer(-1), TextUtil.getInt("-"));
    assertNull("getInt should work with an empty string", TextUtil.getInt(""));
    assertNull("getInt should work with a null value", TextUtil.getInt(null));
  }

  @Test
  public void getLong() throws Exception {
    assertEquals("getLong should work with a digit string", new Long(4), TextUtil.getLong("4"));
    assertEquals("getLong should work with single minus sign", new Long(-1), TextUtil.getLong("-"));
    assertNull("getLong should work with an empty string", TextUtil.getLong(""));
    assertNull("getLong should work with a null value", TextUtil.getLong(null));
  }

  @Test
  public void getDelimitedString() {
    final String result = "test\ttest2" + Util.LINE_SEPARATOR + "data1\tdata2" + Util.LINE_SEPARATOR + "data3\tdata4" + Util.LINE_SEPARATOR;
    assertEquals(result, TextUtil.getDelimitedString(new String[][]{new String[]{"test", "test2"}},
            new String[][]{new String[]{"data1", "data2"}, new String[]{"data3", "data4"}}, "\t"));
  }

  @Test
  public void getTextFileContents() throws IOException {
    final String contents = "<project name=\"jminor-common-core\">" + Util.LINE_SEPARATOR +
            "  <import file=\"../../../build-module.xml\"/>" + Util.LINE_SEPARATOR +
            "</project>" + Util.LINE_SEPARATOR;
    assertEquals(contents, TextUtil.getTextFileContents("modules/common-core/build.xml", Charset.defaultCharset()));
  }

  @Test
  public void collate() {
    final String one = "Bláskuggi";
    final String two = "Blá skuggi";
    final String three = "Blár skuggi";
    final List<String> values = Arrays.asList(one, two, three);
    TextUtil.collate(values);
    assertEquals(0, values.indexOf(two));
    assertEquals(1, values.indexOf(three));
    assertEquals(2, values.indexOf(one));
  }

  @Test
  public void collateSansSpaces() {
    final String b = "Björn Darri";
    final String bNoSpace = "BjörnDarri";
    final String d = "Davíð Arnar";
    final String dNoSpace = "DavíðArnar";
    final String a = "Arnór Jón";
    final List<String> items = Arrays.asList(b, d, a, bNoSpace, dNoSpace);
    TextUtil.collateSansSpaces(Collator.getInstance(), items);
    assertEquals(0, items.indexOf(a));
    assertEquals(1, items.indexOf(b));
    assertEquals(2, items.indexOf(bNoSpace));
    assertEquals(3, items.indexOf(d));
    assertEquals(4, items.indexOf(dNoSpace));
  }

  @Test
  public void getArrayContentsAsString() throws Exception {
    assertEquals("", TextUtil.getArrayContentsAsString(null, true));
    String res = TextUtil.getArrayContentsAsString(new Object[] {1, 2,new Object[] {3, 4}}, false);
    assertEquals("Integer array as string should work", "1, 2, 3, 4", res);
    res = TextUtil.getArrayContentsAsString(new Object[] {1, 2,new Object[] {3, 4}}, true);
    assertEquals("Integer array as string should work", "1\n2\n3\n4\n", res);
  }

  @Test
  public void getListContentsAsString() throws Exception {
    final List<Integer> list = new ArrayList<>();
    list.add(1);
    list.add(2);
    list.add(3);
    list.add(4);
    final String res = TextUtil.getCollectionContentsAsString(list, false);
    assertEquals("Integer list as string should work", "1, 2, 3, 4", res);
  }
}
