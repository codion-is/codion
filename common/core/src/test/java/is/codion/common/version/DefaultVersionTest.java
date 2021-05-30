/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.version;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public final class DefaultVersionTest {

  @Test
  void parse() {
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
  void parseIllegalNull() {
    assertThrows(IllegalArgumentException.class, () -> Version.parse(null));
  }

  @Test
  void parseIllegalEmpty() {
    assertThrows(IllegalArgumentException.class, () -> Version.parse(""));
  }

  @Test
  void constructor() {
    Version version = Version.version(1);
    assertEquals(version.getMajor(), 1);
    assertEquals(version.getMinor(), 0);
    assertEquals(version.getPatch(), 0);

    version = Version.version(1, 0, 23);
    assertEquals(version.getMajor(), 1);
    assertEquals(version.getMinor(), 0);
    assertEquals(version.getPatch(), 23);

    version = Version.version(0, 2, 1);
    assertEquals(version.getMajor(), 0);
    assertEquals(version.getMinor(), 2);
    assertEquals(version.getPatch(), 1);
  }

  @Test
  void compare() {
    final Version version0 = Version.version(0, 0, 1);
    final Version version1 = Version.version(0, 1, 0);
    final Version version2 = Version.version(0, 1, 1);
    final Version version3 = Version.version(1, 0, 1);
    final Version version4 = Version.version(1, 1, 0);

    final List<Version> versions = asList(version3, version4, version0, version2, version1);
    Collections.sort(versions);

    assertEquals(0, versions.indexOf(version0));
    assertEquals(1, versions.indexOf(version1));
    assertEquals(2, versions.indexOf(version2));
    assertEquals(3, versions.indexOf(version3));
    assertEquals(4, versions.indexOf(version4));
  }

  @Test
  void equalsHashCodeToString() {
    final Version version0 = Version.version(2, 1, 5, "RC");
    final Version version1 = Version.parse("2.1.5");
    assertNotEquals(version0, version1);
    assertNotEquals(version0.hashCode(), version1.hashCode());
    assertEquals("2.1.5-RC", version0.toString());
    assertEquals("2.1.5", version1.toString());
    assertEquals(version1, Version.version(2, 1, 5));
    assertNotEquals(version0, Version.version(2, 1, 5, "SNAPSHOT"));

    final Version v111 = Version.version(1, 1, 1);
    final Version v111rc = Version.version(1, 1, 1, "RC");
    final Version v111snap = Version.version(1, 1, 1, "snapshot");

    final Version v121 = Version.version(1, 2, 1);
    final Version v121rc = Version.version(1, 2, 1, "RC");
    final Version v121snap = Version.version(1, 2, 1, "snapshot");

    final Version v112 = Version.version(1, 1, 2);

    final List<Version> versions = new ArrayList<>(Arrays.asList(v112, v121, v121snap, v121rc, v111rc, v111snap, v111));

    Collections.sort(versions);

    assertEquals(0, versions.indexOf(v111rc));
    assertEquals(1, versions.indexOf(v111snap));
    assertEquals(2, versions.indexOf(v111));
    assertEquals(3, versions.indexOf(v112));
    assertEquals(4, versions.indexOf(v121rc));
    assertEquals(5, versions.indexOf(v121snap));
    assertEquals(6, versions.indexOf(v121));
  }

  @Test
  void constructorIllegalMajor() {
    assertThrows(IllegalArgumentException.class, () -> Version.version(-1, 0, 0));
  }

  @Test
  void constructorIllegalMinor() {
    assertThrows(IllegalArgumentException.class, () -> Version.version(0, -1, 0));
  }

  @Test
  void constructorIllegalPatch() {
    assertThrows(IllegalArgumentException.class, () -> Version.version(0, 0, -1));
  }
}
