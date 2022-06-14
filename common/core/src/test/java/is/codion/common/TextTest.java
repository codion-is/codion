/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public final class TextTest {

  @Test
  void spaceAwareCollator() {
    final String one = "björn";
    final String two = "bjö rn";
    final String three = "björ n";
    List<String> strings = asList(one, two, three);

    Comparator<String> collator = Text.getSpaceAwareCollator();
    strings.sort(collator);
    assertEquals(two, strings.get(0));
    assertEquals(three, strings.get(1));
    assertEquals(one, strings.get(2));

    final String four = "tha";
    final String five = "þe";
    final String six = "æi";
    final String seven = "aj";
    strings = asList(four, five, six, seven);

    collator = Text.getSpaceAwareCollator(new Locale("is"));
    strings.sort(collator);
    assertEquals(seven, strings.get(0));
    assertEquals(four, strings.get(1));
    assertEquals(five, strings.get(2));
    assertEquals(six, strings.get(3));

    collator = Text.getSpaceAwareCollator(new Locale("en"));
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
    final String string = "hello";
    assertEquals("hello", Text.padString(string, 4, '*', Text.Alignment.LEFT));
    assertEquals("hello", Text.padString(string, 5, '*', Text.Alignment.LEFT));
    assertEquals("***hello", Text.padString(string, 8, '*', Text.Alignment.LEFT));
    assertEquals("hello***", Text.padString(string, 8, '*', Text.Alignment.RIGHT));
  }

  @Test
  void getDelimitedString() {
    String result = "test\ttest2" + Separators.LINE_SEPARATOR + "data1\tdata2" + Separators.LINE_SEPARATOR + "data3\tdata4";
    assertEquals(result, Text.getDelimitedString(asList("test", "test2"),
            asList(asList("data1", "data2"), asList("data3", "data4")), "\t"));
  }

  @Test
  void getTextFileContents() throws IOException {
    String contents = "here is" + Separators.LINE_SEPARATOR + "some text";
    assertEquals(contents, Text.getTextFileContents("src/test/java/is/codion/common/TextUtilTest.txt", Charset.defaultCharset()));
  }

  @Test
  void collate() {
    final String one = "Bláskuggi";
    final String two = "Blá skuggi";
    final String three = "Blár skuggi";
    List<String> values = asList(one, two, three);
    Text.collate(values);
    assertEquals(0, values.indexOf(two));
    assertEquals(1, values.indexOf(three));
    assertEquals(2, values.indexOf(one));
  }

  @Test
  void collateSansSpaces() {
    final String b = "Björn Darri";
    final String bNoSpace = "BjörnDarri";
    final String d = "Davíð Arnar";
    final String dNoSpace = "DavíðArnar";
    final String a = "Arnór Jón";
    List<String> items = asList(b, d, a, bNoSpace, dNoSpace);
    Text.collateSansSpaces(Collator.getInstance(), items);
    assertEquals(0, items.indexOf(a));
    assertEquals(1, items.indexOf(b));
    assertEquals(2, items.indexOf(bNoSpace));
    assertEquals(3, items.indexOf(d));
    assertEquals(4, items.indexOf(dNoSpace));
  }

  @Test
  void underscoreToCamelCase() {
    assertEquals("", Text.underscoreToCamelCase(""));
    assertEquals("noOfSpeakers", Text.underscoreToCamelCase("noOfSpeakers"));
    assertEquals("noOfSpeakers", Text.underscoreToCamelCase("no_of_speakers"));
    assertEquals("noOfSpeakers", Text.underscoreToCamelCase("No_OF_speakeRS"));
    assertEquals("helloWorld", Text.underscoreToCamelCase("hello_World"));
    assertEquals("", Text.underscoreToCamelCase("_"));
    assertEquals("aB", Text.underscoreToCamelCase("a_b"));
    assertEquals("aB", Text.underscoreToCamelCase("a_b_"));
    assertEquals("aBC", Text.underscoreToCamelCase("a_b_c"));
    assertEquals("aBaC", Text.underscoreToCamelCase("a_ba_c"));
    assertEquals("a", Text.underscoreToCamelCase("a__"));
    assertEquals("a", Text.underscoreToCamelCase("__a"));
  }
}
