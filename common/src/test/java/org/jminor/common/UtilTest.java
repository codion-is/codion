/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public final class UtilTest {

  @Test
  public void testRejectNullValue() {
    Util.rejectNullValue("value", "value");
    try {
      Util.rejectNullValue(null, "value");
    }
    catch (final IllegalArgumentException ignored) {/*ignored*/}
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
  public void equal() throws Exception {
    assertTrue("Two null values should be equal", Util.equal(null, null));
  }

  @Test
  public void getArrayContentsAsString() throws Exception {
    assertEquals("", Util.getArrayContentsAsString(null, true));
    String res = Util.getArrayContentsAsString(new Object[] {1, 2,new Object[] {3, 4}}, false);
    assertEquals("Integer array as string should work", "1, 2, 3, 4", res);
    res = Util.getArrayContentsAsString(new Object[] {1, 2,new Object[] {3, 4}}, true);
    assertEquals("Integer array as string should work", "1\n2\n3\n4\n", res);
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
  public void getTextFileContents() throws IOException {
    final String contents = "<project name=\"jminor-common-core\">" + Util.LINE_SEPARATOR +
            "  <import file=\"../../../build-module.xml\"/>" + Util.LINE_SEPARATOR +
            "</project>" + Util.LINE_SEPARATOR;
    assertEquals(contents, Util.getTextFileContents("modules/common-core/build.xml", Charset.defaultCharset()));
  }

  @Test
  public void notNull() throws Exception {
    assertTrue(Util.notNull(new Object(), new Object(), new Object()));
    assertTrue(Util.notNull(new Object()));
    assertFalse(Util.notNull(new Object(), null, new Object()));
    final Object ob = null;
    assertFalse(Util.notNull(ob));
    assertFalse(Util.notNull((Object[]) null));
  }

  @Test
  public void onClasspath() {
    assertTrue(Util.onClasspath(UtilTest.class.getName()));
    assertFalse(Util.onClasspath("no.class.Here"));
  }

  @Test
  public void nullOrEmpty() {
    assertTrue(Util.nullOrEmpty((Collection[]) null));
    assertTrue(Util.nullOrEmpty(Collections.singletonList(""), null));
    assertTrue(Util.nullOrEmpty(Collections.singletonList(""), Collections.emptyList()));

    final Map<Integer, String> map = new HashMap<>();
    map.put(1, "1");
    assertTrue(Util.nullOrEmpty((Map[]) null));
    assertTrue(Util.nullOrEmpty(map, null));
    assertTrue(Util.nullOrEmpty(map, Collections.emptyMap()));

    assertTrue(Util.nullOrEmpty((String[]) null));
    assertTrue(Util.nullOrEmpty("sadf", null));
    assertTrue(Util.nullOrEmpty("asdf", ""));

    assertFalse(Util.nullOrEmpty(Collections.singletonList("1")));
    assertFalse(Util.nullOrEmpty(Arrays.asList("1", "2")));

    assertFalse(Util.nullOrEmpty("asdf"));
    assertFalse(Util.nullOrEmpty("asdf", "wefs"));

    assertFalse(Util.nullOrEmpty(map));
  }
}
