/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public final class TextUtilTest {

  @Test
  public void spaceAwareCollator() {
    final String one = "björn";
    final String two = "bjö rn";
    final String three = "björ n";
    List<String> strings = asList(one, two, three);

    Comparator<String> collator = TextUtil.getSpaceAwareCollator();
    strings.sort(collator);
    assertEquals(two, strings.get(0));
    assertEquals(three, strings.get(1));
    assertEquals(one, strings.get(2));

    final String four = "tha";
    final String five = "þe";
    final String six = "æi";
    final String seven = "aj";
    strings = asList(four, five, six, seven);

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
    assertThrows(IllegalArgumentException.class, () -> TextUtil.createRandomString(3, 2));
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
    final List<String> values = asList(one, two, three);
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
    final List<String> items = asList(b, d, a, bNoSpace, dNoSpace);
    TextUtil.collateSansSpaces(Collator.getInstance(), items);
    assertEquals(0, items.indexOf(a));
    assertEquals(1, items.indexOf(b));
    assertEquals(2, items.indexOf(bNoSpace));
    assertEquals(3, items.indexOf(d));
    assertEquals(4, items.indexOf(dNoSpace));
  }
}
