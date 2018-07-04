/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class VersionTest {

  @Test
  public void parse() {
    Version version = Version.parse("2.12.4 2018.01.02 15:27");
    assertEquals(version.getMajor(), 2);
    assertEquals(version.getMinor(), 12);
    assertEquals(version.getPatch(), 4);
    assertEquals("2018.01.02 15:27", version.getMetadata());

    version = Version.parse("2.12.4");
    assertEquals(version.getMajor(), 2);
    assertEquals(version.getMinor(), 12);
    assertEquals(version.getPatch(), 4);

    version = Version.parse("0.2-build 23");
    assertEquals(version.getMajor(), 0);
    assertEquals(version.getMinor(), 2);
    assertEquals(version.getPatch(), 0);
    assertEquals("build 23", version.getMetadata());

    version = Version.parse("1-RC");
    assertEquals(version.getMajor(), 1);
    assertEquals(version.getMinor(), 0);
    assertEquals(version.getPatch(), 0);
    assertEquals("RC", version.getMetadata());
  }

  @Test
  public void parseIllegalNull() {
    assertThrows(IllegalArgumentException.class, () -> Version.parse(null));
  }

  @Test
  public void parseIllegalEmpty() {
    assertThrows(IllegalArgumentException.class, () -> Version.parse(""));
  }

  @Test
  public void constructor() {
    Version version = new Version(1);
    assertEquals(version.getMajor(), 1);
    assertEquals(version.getMinor(), 0);
    assertEquals(version.getPatch(), 0);

    version = new Version(1, 0, 23);
    assertEquals(version.getMajor(), 1);
    assertEquals(version.getMinor(), 0);
    assertEquals(version.getPatch(), 23);

    version = new Version(0, 2, 1);
    assertEquals(version.getMajor(), 0);
    assertEquals(version.getMinor(), 2);
    assertEquals(version.getPatch(), 1);
  }

  @Test
  public void compare() {
    final Version version0 = new Version(0, 0, 1);
    final Version version1 = new Version(0, 1, 0);
    final Version version2 = new Version(0, 1, 1);
    final Version version3 = new Version(1, 0, 1);
    final Version version4 = new Version(1, 1, 0);

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
    final Version version0 = new Version(2, 1, 5, "RC");
    final Version version1 = Version.parse("2.1.5");
    assertEquals(version0, version1);
    assertEquals(version0.hashCode(), version1.hashCode());
    assertEquals("2.1.5-RC", version0.toString());
    assertEquals("2.1.5", version1.toString());
  }

  @Test
  public void constructorIllegalMajor() {
    assertThrows(IllegalArgumentException.class, () -> new Version(-1, 0, 0));
  }

  @Test
  public void constructorIllegalMinor() {
    assertThrows(IllegalArgumentException.class, () -> new Version(0, -1, 0));
  }

  @Test
  public void constructorIllegalPatch() {
    assertThrows(IllegalArgumentException.class, () -> new Version(0, 0, -1));
  }
}
