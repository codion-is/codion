/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.junit.Test;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadFactory;

import static org.junit.Assert.*;

public class UtilTest {

  @Test
  public void parseConfigurationFile() throws IOException {
    File mainConfigurationFile = null;
    File secondConfigurationFile = null;
    File thirdConfigurationFile = null;
    try {
      //Create three config files, the first references the second which references the third
      final File userDir = new File(System.getProperty("user.dir"));
      mainConfigurationFile = new File(userDir, "UtilTestMain.config");
      secondConfigurationFile = new File(userDir, "UtilTestSecond.config");
      thirdConfigurationFile = new File(userDir, "UtilTestThird.config");

      Properties properties = new Properties();
      properties.put("main.property", "value");
      properties.put(Util.ADDITIONAL_CONFIGURATION_FILES, "UtilTestSecond.config");
      try (final FileOutputStream outputStream = new FileOutputStream(mainConfigurationFile)) {
        properties.store(outputStream, "");
      }

      properties = new Properties();
      properties.put("second.property", "value");
      properties.put(Util.ADDITIONAL_CONFIGURATION_FILES, "UtilTestThird.config");
      try (final FileOutputStream outputStream = new FileOutputStream(secondConfigurationFile)) {
        properties.store(outputStream, "");
      }

      properties = new Properties();
      properties.put("third.property", "value");
      try (final FileOutputStream outputStream = new FileOutputStream(thirdConfigurationFile)) {
        properties.store(outputStream, "");
      }

      //done prep, now the test
      Util.parseConfigurationFile("UtilTestMain.config");
      assertEquals("value", System.getProperty("main.property"));
      assertEquals("value", System.getProperty("second.property"));
      assertEquals("value", System.getProperty("third.property"));
    }
    finally {
      try {
        if (mainConfigurationFile != null) {
          mainConfigurationFile.delete();
        }
      }
      catch (Exception ignored) {}
      try {
        if (secondConfigurationFile != null) {
          secondConfigurationFile.delete();
        }
      }
      catch (Exception ignored) {}
      try {
        if (thirdConfigurationFile != null) {
          thirdConfigurationFile.delete();
        }
      }
      catch (Exception ignored) {}
    }
  }

  @Test
  public void spaceAwareCollator() {
    final String one = "björn";
    final String two = "bjö rn";
    final String three = "björ n";
    final List<String> strings = Arrays.asList(one, two, three);

    final Comparator<String> collator = Util.getSpaceAwareCollator();

    Collections.sort(strings, collator);
    assertEquals(two, strings.get(0));
    assertEquals(three, strings.get(1));
    assertEquals(one, strings.get(2));
  }

  @Test
  public void testRejectNullValue() {
    Util.rejectNullValue("value", "value");
    try {
      Util.rejectNullValue(null, "value");
    }
    catch (IllegalArgumentException e) {}
  }

  @Test
  public void isEqual() {
    Object one = null;
    Object two = null;
    assertTrue(Util.equal(one, two));

    one = new Object();
    assertFalse(Util.equal(one, two));
    two = new Object();
    assertFalse(Util.equal(one, two));

    two = one;
    assertTrue(Util.equal(one, two));
  }

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

  @Test(expected = IllegalArgumentException.class)
  public void createRandomStringMinLengthExceedsMaxLength() {
    Util.createRandomString(3, 2);
  }

  @Test
  public void createRandomString() {
    String randomString = Util.createRandomString(1, 1);
    assertEquals(1, randomString.length());
    randomString = Util.createRandomString(5, 5);
    assertEquals(5, randomString.length());
    randomString = Util.createRandomString(4, 10);
    assertTrue(randomString.length() >= 4);
    assertTrue(randomString.length() <= 10);
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
    assertNull("getInt should work with a null value", Util.getInt(null));
  }

  @Test
  public void getLong() throws Exception {
    assertEquals("getLong should work with a digit string", new Long(4), Util.getLong("4"));
    assertEquals("getLong should work with single minus sign", new Long(-1), Util.getLong("-"));
    assertNull("getLong should work with an empty string", Util.getLong(""));
    assertNull("getLong should work with a null value", Util.getInt(null));
  }

  @Test
  public void getListContentsAsString() throws Exception {
    final List<Integer> list = new ArrayList<>();
    list.add(1);
    list.add(2);
    list.add(3);
    list.add(4);
    final String res = Util.getCollectionContentsAsString(list, false);
    assertEquals("Integer list as string should work", "1, 2, 3, 4", res);
  }

  @Test
  public void countLines() throws IOException {
    assertEquals(3, Util.countLines(new File("resources/security/all_permissions.policy")));
    assertEquals(2, Util.countLines(new File("resources/security/all_permissions.policy"), "}"));
  }

  @Test
  public void getTextFileContents() throws IOException {
    final String contents = "grant {" + Util.LINE_SEPARATOR +
            "  permission java.security.AllPermission;" + Util.LINE_SEPARATOR +
            "};" + Util.LINE_SEPARATOR;
    assertEquals(contents, Util.getTextFileContents("resources/security/all_permissions.policy", Charset.defaultCharset()));
  }

  @Test
  public void getDelimitedString() {
    final String result = "test\ttest2" + Util.LINE_SEPARATOR + "data1\tdata2" + Util.LINE_SEPARATOR + "data3\tdata4" + Util.LINE_SEPARATOR;
    assertEquals(result, Util.getDelimitedString(new String[][]{new String[]{"test", "test2"}},
            new String[][]{new String[]{"data1", "data2"}, new String[]{"data3", "data4"}}, "\t"));
  }

  @Test
  public void notNull() throws Exception {
    assertTrue(Util.notNull(new Object(), new Object(), new Object()));
    assertTrue(Util.notNull(new Object()));
    assertFalse(Util.notNull(new Object(), null, new Object()));
    final Object ob = null;
    assertFalse(Util.notNull(ob));
  }

  @Test
  public void closeSilently() {
    Util.closeSilently((Closeable) null);
    Util.closeSilently((Closeable[]) null);
    Util.closeSilently(null, null);
  }

  @Test
  public void collateSansSpaces() {
    final String b = "Björn Darri";
    final String bNoSpace = "BjörnDarri";
    final String d = "Davíð Arnar";
    final String dNoSpace = "DavíðArnar";
    final String a = "Arnór Jón";
    final List<String> items = Arrays.asList(b, d, a, bNoSpace, dNoSpace);
    Util.collateSansSpaces(Collator.getInstance(), items);
    assertEquals(0, items.indexOf(a));
    assertEquals(1, items.indexOf(b));
    assertEquals(2, items.indexOf(bNoSpace));
    assertEquals(3, items.indexOf(d));
    assertEquals(4, items.indexOf(dNoSpace));
  }

  @Test
  public void onClasspath() {
    assertTrue(Util.onClasspath(UtilTest.class.getName()));
    assertFalse(Util.onClasspath("no.class.Here"));
  }

  @Test
  public void nullOrEmpty() {
    assertTrue(Util.nullOrEmpty((Collection[]) null));
    assertTrue(Util.nullOrEmpty(Arrays.asList(""), null));
    assertTrue(Util.nullOrEmpty(Arrays.asList(""), Collections.emptyList()));

    final Map<Integer, String> map = new HashMap<>();
    map.put(1, "1");
    assertTrue(Util.nullOrEmpty((Map[]) null));
    assertTrue(Util.nullOrEmpty(map, null));
    assertTrue(Util.nullOrEmpty(map, Collections.emptyMap()));

    assertTrue(Util.nullOrEmpty((String[]) null));
    assertTrue(Util.nullOrEmpty("sadf", null));
    assertTrue(Util.nullOrEmpty("asdf", ""));

    assertFalse(Util.nullOrEmpty(Arrays.asList("1")));
    assertFalse(Util.nullOrEmpty(Arrays.asList("1", "2")));

    assertFalse(Util.nullOrEmpty("asdf"));
    assertFalse(Util.nullOrEmpty("asdf", "wefs"));

    assertFalse(Util.nullOrEmpty(map));
  }

  @Test
  public void daemonThreadFactory() {
    final ThreadFactory factory = new Util.DaemonThreadFactory();
    final Thread thread = factory.newThread(new Runnable() {
      @Override
      public void run() {}
    });
    assertTrue(thread.isDaemon());
  }

  @Test
  public void getGetMethod() throws NoSuchMethodException {
    final Bean bean = new Bean();
    Method getMethod = Util.getGetMethod(boolean.class, "booleanValue", bean);
    assertEquals("isBooleanValue", getMethod.getName());
    getMethod = Util.getGetMethod(int.class, "intValue", bean);
    assertEquals("getIntValue", getMethod.getName());
  }

  @Test
  public void getSetMethod() throws NoSuchMethodException {
    final Bean bean = new Bean();
    Method setMethod = Util.getSetMethod(boolean.class, "booleanValue", bean);
    assertEquals("setBooleanValue", setMethod.getName());
    setMethod = Util.getSetMethod(int.class, "intValue", bean);
    assertEquals("setIntValue", setMethod.getName());
  }

  @Test(expected = NoSuchMethodException.class)
  public void getGetMethodInvalidMethod() throws NoSuchMethodException {
    final Bean bean = new Bean();
    Util.getGetMethod(boolean.class, "invalidValue", bean);
  }

  @Test(expected = NoSuchMethodException.class)
  public void getSetMethodInvalidMethod() throws NoSuchMethodException {
    final Bean bean = new Bean();
    Util.getSetMethod(boolean.class, "invalidValue", bean);
  }

  @Test
  public void collate() {
    final String one = "Bláskuggi";
    final String two = "Blá skuggi";
    final String three = "Blár skuggi";
    final List<String> values = Arrays.asList(one, two, three);
    Util.collate(values);
    assertEquals(0, values.indexOf(two));
    assertEquals(1, values.indexOf(three));
    assertEquals(2, values.indexOf(one));
  }

  public static final class Bean {
    boolean booleanValue;
    int intValue;

    public boolean isBooleanValue() {
      return booleanValue;
    }

    public void setBooleanValue(final boolean booleanValue) {
      this.booleanValue = booleanValue;
    }

    public int getIntValue() {
      return intValue;
    }

    public void setIntValue(final int intValue) {
      this.intValue = intValue;
    }
  }
}
