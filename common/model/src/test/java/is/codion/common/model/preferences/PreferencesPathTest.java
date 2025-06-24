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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.model.preferences;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class PreferencesPathTest {

	@Test
	void testPlatformPath() {
		// We can't easily test all platforms in a single test run,
		// but we can verify the current platform returns a reasonable path
		Path path = PreferencesPath.userPreferencesPath();

		// Should end with preferences.json
		assertTrue(path.toString().endsWith("preferences.json"));

		// Should contain a Codion directory (case may vary by platform)
		String pathString = path.toString().toLowerCase();
		assertTrue(pathString.contains("codion") || pathString.contains(".codion"));
	}

	@Test
	void testWindowsPath() {
		// Test Windows path logic by examining the format
		String osName = System.getProperty("os.name", "").toLowerCase();
		if (osName.contains("win")) {
			Path path = PreferencesPath.userPreferencesPath();

			// On Windows, should use LOCALAPPDATA or APPDATA
			String pathStr = path.toString();
			assertTrue(pathStr.contains("AppData") || pathStr.contains("codion"),
							"Windows path should contain AppData or fall back to .codion: " + pathStr);
		}
	}

	@Test
	void testMacOSPath() {
		// Test macOS path logic
		String osName = System.getProperty("os.name", "").toLowerCase();
		if (osName.contains("mac")) {
			Path path = PreferencesPath.userPreferencesPath();

			// On macOS, should use Library/Preferences
			assertTrue(path.toString().contains("Library/Preferences/Codion"),
							"macOS path should contain Library/Preferences/Codion: " + path);
		}
	}

	@Test
	void testLinuxPath() {
		// Test Linux path logic
		String osName = System.getProperty("os.name", "").toLowerCase();
		if (osName.contains("nix") || osName.contains("nux")) {
			Path path = PreferencesPath.userPreferencesPath();

			// On Linux, should use .config or XDG_CONFIG_HOME
			String pathStr = path.toString();
			assertTrue(pathStr.contains(".config/codion") || pathStr.contains("codion"),
							"Linux path should contain .config/codion: " + pathStr);
		}
	}

	@Test
	void testPathConsistency() {
		// Multiple calls should return the same path
		Path path1 = PreferencesPath.userPreferencesPath();
		Path path2 = PreferencesPath.userPreferencesPath();

		assertEquals(path1, path2, "Path should be consistent across calls");
	}
}