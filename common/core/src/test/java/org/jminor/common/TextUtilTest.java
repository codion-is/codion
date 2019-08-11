/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

public final class TextUtilTest {

  @Test
  public void spaceAwareCollator() {
    final String one = "björn";
    final String two = "bjö rn";
    final String three = "björ n";
    List<String> strings = Arrays.asList(one, two, three);

    Comparator<String> collator = TextUtil.getSpaceAwareCollator();
    strings.sort(collator);
    assertEquals(two, strings.get(0));
    assertEquals(three, strings.get(1));
    assertEquals(one, strings.get(2));

    final String four = "tha";
    final String five = "þe";
    final String six = "æi";
    final String seven = "aj";
    strings = Arrays.asList(four, five, six, seven);

    collator = TextUtil.getSpaceAwareCollator(new Locale("is"));
    strings.sort(collator);
    assertEquals(seven, strings.get(0));
    assertEquals(four, strings.get(1));
    assertEquals(five, strings.get(2));
    assertEquals(six, strings.get(3));

    collator = TextUtil.getSpaceAwareCollator(new Locale("en"));
    strings.sort(collator);
    assertEquals(six, strings.get(0));
    assertEquals(seven, strings.get(1));
    assertEquals(four, strings.get(2));
    assertEquals(five, strings.get(3));
  }

  @Test
  public void createRandomStringMinLengthExceedsMaxLength() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> TextUtil.createRandomString(3, 2));
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
    assertEquals("hello", TextUtil.padString(string, 4, '*', TextUtil.Alignment.LEFT));
    assertEquals("hello", TextUtil.padString(string, 5, '*', TextUtil.Alignment.LEFT));
    assertEquals("***hello", TextUtil.padString(string, 8, '*', TextUtil.Alignment.LEFT));
    assertEquals("hello***", TextUtil.padString(string, 8, '*', TextUtil.Alignment.RIGHT));
  }

  @Test
  public void getDouble() throws Exception {
    assertEquals(Double.valueOf(4.22), TextUtil.getDouble("4,22"), "getDouble should work with comma");
    assertEquals(Double.valueOf(4.22), TextUtil.getDouble("4.22"), "getDouble should work with period");
    assertEquals(Double.valueOf(-1), TextUtil.getDouble("-"), "getDouble should work with single minus sign");
    assertNull(TextUtil.getDouble(""), "getDouble should work with an empty string");
  }

  @Test
  public void getInt() throws Exception {
    assertEquals(Integer.valueOf(4), TextUtil.getInt("4"), "getInt should work with a digit string");
    assertEquals(Integer.valueOf(-1), TextUtil.getInt("-"), "getInt should work with single minus sign");
    assertNull(TextUtil.getInt(""), "getInt should work with an empty string");
    assertNull(TextUtil.getInt(null), "getInt should work with a null value");
  }

  @Test
  public void getLong() throws Exception {
    assertEquals(Long.valueOf(4), TextUtil.getLong("4"), "getLong should work with a digit string");
    assertEquals(Long.valueOf(-1), TextUtil.getLong("-"), "getLong should work with single minus sign");
    assertNull(TextUtil.getLong(""), "getLong should work with an empty string");
    assertNull(TextUtil.getLong(null), "getLong should work with a null value");
  }

  @Test
  public void getDelimitedString() {
    final String result = "test\ttest2" + Util.LINE_SEPARATOR + "data1\tdata2" + Util.LINE_SEPARATOR + "data3\tdata4";
    assertEquals(result, TextUtil.getDelimitedString(new String[][] {new String[] {"test", "test2"}},
            new String[][] {new String[] {"data1", "data2"}, new String[] {"data3", "data4"}}, "\t"));
  }

  @Test
  public void getTextFileContents() throws IOException {
    final String contents = "here is" + Util.LINE_SEPARATOR + "some text";
    assertEquals(contents, TextUtil.getTextFileContents("src/test/java/org/jminor/common/TextUtilTest.txt", Charset.defaultCharset()));
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
    String res = TextUtil.getArrayContentsAsString(new Object[] {1, 2, new Object[] {3, 4}}, false);
    assertEquals("1, 2, 3, 4", res, "Integer array as string should work");
    res = TextUtil.getArrayContentsAsString(new Object[] {1, 2, new Object[] {3, 4}}, true);
    assertEquals("1\n2\n3\n4\n", res, "Integer array as string should work");
  }

  @Test
  public void getListContentsAsString() throws Exception {
    final List<Integer> list = new ArrayList<>();
    list.add(1);
    list.add(2);
    list.add(3);
    list.add(4);
    final String res = TextUtil.getCollectionContentsAsString(list, false);
    assertEquals("1, 2, 3, 4", res, "Integer list as string should work");
  }
}
