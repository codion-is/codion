/*
 * Copyright (c) 2015 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.version;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public final class VersionTest {

  @Test
  void parse() {
    Version version = Version.parse("2.12.4 2018.01.02 15:27");
    assertEquals(version.major(), 2);
    assertEquals(version.minor(), 12);
    assertEquals(version.patch(), 4);
    assertEquals("2018.01.02 15:27", version.metadata());

    version = Version.parse("2.12.4");
    assertEquals(version.major(), 2);
    assertEquals(version.minor(), 12);
    assertEquals(version.patch(), 4);

    version = Version.parse("0.2-build 23");
    assertEquals(version.major(), 0);
    assertEquals(version.minor(), 2);
    assertEquals(version.patch(), 0);
    assertEquals("build 23", version.metadata());

    version = Version.parse("1-RC");
    assertEquals(version.major(), 1);
    assertEquals(version.minor(), 0);
    assertEquals(version.patch(), 0);
    assertEquals("RC", version.metadata());
  }

  @Test
  void parsePropertiesFile() {
    assertThrows(IllegalArgumentException.class, () -> Version.parsePropertiesFile(Version.class, "/version_non_existing.properties"));
    assertThrows(IllegalArgumentException.class, () -> Version.parsePropertiesFile(Version.class, "/version_invalid_test.properties"));
    Version version = Version.parsePropertiesFile(Version.class, "/version_test.properties");
    assertEquals(version, Version.version(0, 1, 0));
    assertThrows(NullPointerException.class, () -> Version.parsePropertiesFile(null, "/version_test.properties"));
    assertThrows(NullPointerException.class, () -> Version.parsePropertiesFile(Version.class, null));
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
    assertEquals(version.major(), 1);
    assertEquals(version.minor(), 0);
    assertEquals(version.patch(), 0);

    version = Version.version(1, 0, 23);
    assertEquals(version.major(), 1);
    assertEquals(version.minor(), 0);
    assertEquals(version.patch(), 23);

    version = Version.version(0, 2, 1);
    assertEquals(version.major(), 0);
    assertEquals(version.minor(), 2);
    assertEquals(version.patch(), 1);
  }

  @Test
  void compare() {
    Version version0 = Version.version(0, 0, 1);
    Version version1 = Version.version(0, 1, 0);
    Version version2 = Version.version(0, 1, 1);
    Version version3 = Version.version(1, 0, 1);
    Version version4 = Version.version(1, 1, 0);

    List<Version> versions = asList(version3, version4, version0, version2, version1);
    Collections.sort(versions);

    assertEquals(0, versions.indexOf(version0));
    assertEquals(1, versions.indexOf(version1));
    assertEquals(2, versions.indexOf(version2));
    assertEquals(3, versions.indexOf(version3));
    assertEquals(4, versions.indexOf(version4));
  }

  @Test
  void equalsHashCodeToString() {
    Version version0 = Version.version(2, 1, 5, "RC");
    Version version1 = Version.parse("2.1.5");
    assertNotEquals(version0, version1);
    assertNotEquals(version0.hashCode(), version1.hashCode());
    assertEquals("2.1.5-RC", version0.toString());
    assertEquals("2.1.5", version1.toString());
    assertEquals(version1, Version.version(2, 1, 5));
    assertNotEquals(version0, Version.version(2, 1, 5, "SNAPSHOT"));

    Version v111 = Version.version(1, 1, 1);
    Version v111rc = Version.version(1, 1, 1, "RC");
    Version v111snap = Version.version(1, 1, 1, "snapshot");

    Version v121 = Version.version(1, 2, 1);
    Version v121rc = Version.version(1, 2, 1, "RC");
    Version v121snap = Version.version(1, 2, 1, "snapshot");

    Version v112 = Version.version(1, 1, 2);

    List<Version> versions = new ArrayList<>(Arrays.asList(v112, v121, v121snap, v121rc, v111rc, v111snap, v111));

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
