/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2015 - 2025, Björn Darri Sigurðsson.
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
		Version version = Version.parse("2.12.4+2018.01.02 15:27");
		assertEquals(2, version.major());
		assertEquals(12, version.minor());
		assertEquals(4, version.patch());
		assertEquals("2018.01.02 15:27", version.build().orElse(null));

		version = Version.parse("2.12.4");
		assertEquals(2, version.major());
		assertEquals(12, version.minor());
		assertEquals(4, version.patch());

		version = Version.parse("0.2-RC23");
		assertEquals(0, version.major());
		assertEquals(2, version.minor());
		assertEquals(0, version.patch());
		assertEquals("RC23", version.metadata().orElse(null));

		version = Version.parse("1-RC+10-10");
		assertEquals(1, version.major());
		assertEquals(0, version.minor());
		assertEquals(0, version.patch());
		assertEquals("RC", version.metadata().orElse(null));
		assertEquals("10-10", version.build().orElse(null));
	}

	@Test
	void parsePropertiesFile() {
		assertThrows(IllegalArgumentException.class, () -> Version.parse(Version.class, "/version_non_existing.properties"));
		assertThrows(IllegalArgumentException.class, () -> Version.parse(Version.class, "/version_invalid_test.properties"));
		Version version = Version.parse(Version.class, "/version_test.properties");
		assertEquals(version, Version.builder().minor(1).build());
		assertThrows(NullPointerException.class, () -> Version.parse(null, "/version_test.properties"));
		assertThrows(NullPointerException.class, () -> Version.parse(Version.class, null));
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
		Version version = Version.builder().major(1).build();
		assertEquals(1, version.major());
		assertEquals(0, version.minor());
		assertEquals(0, version.patch());

		version = Version.builder().major(1).patch(23).build();
		assertEquals(1, version.major());
		assertEquals(0, version.minor());
		assertEquals(23, version.patch());

		version = Version.builder().minor(2).patch(1).build();
		assertEquals(0, version.major());
		assertEquals(2, version.minor());
		assertEquals(1, version.patch());
	}

	@Test
	void compare() {
		Version version0 = Version.builder().patch(1).build();
		Version version1 = Version.builder().minor(1).build();
		Version version2 = Version.builder().minor(1).patch(1).build();
		Version version3 = Version.builder().major(1).patch(1).build();
		Version version4 = Version.builder().major(1).minor(1).build();

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
		Version version0 = Version.builder().major(2).minor(1).patch(5).metadata("RC").build();
		Version version1 = Version.parse("2.1.5");
		assertNotEquals(version0, version1);
		assertNotEquals(version0.hashCode(), version1.hashCode());
		assertEquals("2.1.5-RC", version0.toString());
		assertEquals("2.1.5", version1.toString());
		assertEquals(version1, Version.builder().major(2).minor(1).patch(5).build());
		assertNotEquals(version0, Version.builder().major(2).minor(1).patch(5).metadata("SNAPSHOT").build());

		Version v111 = Version.builder().major(1).minor(1).patch(1).build();
		Version v111rc = Version.builder().major(1).minor(1).patch(1).metadata("RC").build();
		Version v111snap = Version.builder().major(1).minor(1).patch(1).metadata("snapshot").build();

		Version v121 = Version.builder().major(1).minor(2).patch(1).build();
		Version v121rc = Version.builder().major(1).minor(2).patch(1).metadata("RC").build();
		Version v121snap = Version.builder().major(1).minor(2).patch(1).metadata("snapshot").build();

		Version v112 = Version.builder().major(1).minor(1).patch(2).build();

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
		assertThrows(IllegalArgumentException.class, () -> Version.builder().major(-1));
	}

	@Test
	void constructorIllegalMinor() {
		assertThrows(IllegalArgumentException.class, () -> Version.builder().minor(-1));
	}

	@Test
	void constructorIllegalPatch() {
		assertThrows(IllegalArgumentException.class, () -> Version.builder().patch(-1));
	}
}
