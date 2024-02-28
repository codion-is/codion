/*
 * Copyright (c) 2016 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

public final class TextTest {

  @Test
  void collator() {
    final String one = "björn";
    final String two = "bjö rn";
    final String three = "björ n";
    List<String> strings = asList(one, two, three);

    Comparator<String> collator = Text.collator();
    strings.sort(collator);
    assertEquals(two, strings.get(0));
    assertEquals(three, strings.get(1));
    assertEquals(one, strings.get(2));

    final String four = "tha";
    final String five = "þe";
    final String six = "æi";
    final String seven = "aj";
    strings = asList(four, five, six, seven);

    collator = Text.collator(new Locale("is"));
    strings.sort(collator);
    assertEquals(seven, strings.get(0));
    assertEquals(four, strings.get(1));
    assertEquals(five, strings.get(2));
    assertEquals(six, strings.get(3));

    collator = Text.collator(new Locale("en"));
    strings.sort(collator);
    assertEquals(six, strings.get(0));
    assertEquals(seven, strings.get(1));
    assertEquals(four, strings.get(2));
    assertEquals(five, strings.get(3));
  }

  @Test
  void randomStringMinLengthExceedsMaxLength() {
    assertThrows(IllegalArgumentException.class, () -> Text.randomString(3, 2));
  }

  @Test
  void randomString() {
    String randomString = Text.randomString(1, 1);
    assertEquals(1, randomString.length());
    randomString = Text.randomString(5, 5);
    assertEquals(5, randomString.length());
    randomString = Text.randomString(4, 10);
    assertTrue(randomString.length() >= 4);
    assertTrue(randomString.length() <= 10);
  }

  @Test
  void padString() {
    String string = "hello";
    assertEquals("hello", Text.leftPad(string, 4, '*'));
    assertEquals("hello", Text.leftPad(string, 5, '*'));
    assertEquals("***hello", Text.leftPad(string, 8, '*'));
    assertEquals("hello***", Text.rightPad(string, 8, '*'));
  }

  @Test
  void delimitedString() {
    String result = "test\ttest2" + Separators.LINE_SEPARATOR + "data1\tdata2" + Separators.LINE_SEPARATOR + "data3\tdata4";
    assertEquals(result, Text.delimitedString(asList("test", "test2"),
            asList(asList("data1", "data2"), asList("data3", "data4")), "\t"));
  }

  @Test
  void textFileContentsResource() throws IOException {
    String contents = "here is" + Separators.LINE_SEPARATOR + "some text";
    assertEquals(contents, Text.textFileContents(TextTest.class, "TextTest.txt"));
    assertThrows(FileNotFoundException.class, () -> Text.textFileContents(TextTest.class, "None.txt"));
  }

  @Test
  void textFileContents() throws IOException {
    String contents = "here is" + Separators.LINE_SEPARATOR + "some text";
    assertEquals(contents, Text.textFileContents("src/test/resources/is/codion/common/TextTest.txt", Charset.defaultCharset()));
  }

  @Test
  void collate() {
    String one = "Bláskuggi";
    String two = "Blá skuggi";
    String three = "Blár skuggi";
    List<String> values = asList(one, two, three);
    Text.collate(values);
    assertEquals(0, values.indexOf(two));
    assertEquals(1, values.indexOf(three));
    assertEquals(2, values.indexOf(one));

    String b = "Björn Darri";
    String bNoSpace = "BjörnDarri";
    String d = "Davíð Arnar";
    String dNoSpace = "DavíðArnar";
    String a = "Arnór Jón";
    values = asList(b, d, a, bNoSpace, dNoSpace);
    Text.collate(values);
    assertEquals(0, values.indexOf(a));
    assertEquals(1, values.indexOf(b));
    assertEquals(2, values.indexOf(bNoSpace));
    assertEquals(3, values.indexOf(d));
    assertEquals(4, values.indexOf(dNoSpace));
  }

  @Test
  void parseCommaSeparatedValues() {
    List<String> hello = Collections.singletonList("hello");
    assertEquals(hello, Text.parseCommaSeparatedValues("hello"));
    assertEquals(hello, Text.parseCommaSeparatedValues("hello, "));
    assertEquals(hello, Text.parseCommaSeparatedValues(",hello , "));
    assertEquals(Arrays.asList("hello", "world", "how", "are", "you"), Text.parseCommaSeparatedValues("hello,world, how , are ,you"));
    assertEquals(emptyList(), Text.parseCommaSeparatedValues(""));
    assertEquals(emptyList(), Text.parseCommaSeparatedValues(", ,  , "));
    assertEquals(emptyList(), Text.parseCommaSeparatedValues(null));
  }
}
