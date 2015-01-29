/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public final class VersionsTest {

  @Test
  public void parse() {
    Version version = Versions.version("2.12.4");
    assertEquals(version.getMajor(), 2);
    assertEquals(version.getMinor(), 12);
    assertEquals(version.getPatch(), 4);

    version = Versions.version("0.2-build 23");
    assertEquals(version.getMajor(), 0);
    assertEquals(version.getMinor(), 2);
    assertEquals(version.getPatch(), 0);
    assertEquals("build 23", version.getMetadata());

    version = Versions.version("1-RC");
    assertEquals(version.getMajor(), 1);
    assertEquals(version.getMinor(), 0);
    assertEquals(version.getPatch(), 0);
    assertEquals("RC", version.getMetadata());
  }

  @Test(expected = IllegalArgumentException.class)
  public void parseIllegalNull() {
    Versions.version(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void parseIllegalEmpty() {
    Versions.version("");
  }

  @Test
  public void constructor() {
    Version version = Versions.version(1);
    assertEquals(version.getMajor(), 1);
    assertEquals(version.getMinor(), 0);
    assertEquals(version.getPatch(), 0);

    version = Versions.version(1, 0, 23);
    assertEquals(version.getMajor(), 1);
    assertEquals(version.getMinor(), 0);
    assertEquals(version.getPatch(), 23);

    version = Versions.version(0, 2, 1);
    assertEquals(version.getMajor(), 0);
    assertEquals(version.getMinor(), 2);
    assertEquals(version.getPatch(), 1);
  }

  @Test
  public void compare() {
    final Version version0 = Versions.version(0, 0, 1);
    final Version version1 = Versions.version(0, 1, 0);
    final Version version2 = Versions.version(0, 1, 1);
    final Version version3 = Versions.version(1, 0, 1);
    final Version version4 = Versions.version(1, 1, 0);

    final List<Version> versions = Arrays.asList(version3, version4, version0, version2, version1);
    Collections.sort(versions);

    assertEquals(0, versions.indexOf(version0));
    assertEquals(1, versions.indexOf(version1));
    assertEquals(2, versions.indexOf(version2));
    assertEquals(3, versions.indexOf(version3));
    assertEquals(4, versions.indexOf(version4));
  }

  @Test
  public void equalsHashCodeToString() {
    final Version version0 = Versions.version(2, 1, 5, "RC");
    final Version version1 = Versions.version("2.1.5");
    assertEquals(version0, version1);
    assertEquals(version0.hashCode(), version1.hashCode());
    assertEquals("2.1.5-RC", version0.toString());
    assertEquals("2.1.5", version1.toString());
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorIllegalMajor() {
    Versions.version(-1, 0, 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorIllegalMinor() {
    Versions.version(0, -1, 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorIllegalPatch() {
    Versions.version(0, 0, -1);
  }
}
