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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.model.preferences;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.Objects.requireNonNull;

/**
 * Provides platform-specific paths for storing preferences.
 */
final class PreferencesPath {

	private static final class PreferencesLocationHolder {
		private static final @Nullable String LOCATION = FilePreferences.PREFERENCES_LOCATION.get();
	}

	private static final String CODION_DIR = "Codion";
	private static final String USER_HOME = "user.home";
	private static final String JSON = ".json";

	private PreferencesPath() {}

	/**
	 * Returns the platform-specific path for storing user preferences.
	 * Uses {@link FilePreferences#PREFERENCES_LOCATION} in case it is specified.
	 * <ul>
	 * <li>Windows: {@code %LOCALAPPDATA%/Codion/{filename}.json}
	 * <li>macOS: {@code ~/Library/Preferences/Codion/{filename}.json}
	 * <li>Linux: {@code ~/.config/codion/{filename}.json} (or {@code $XDG_CONFIG_HOME/codion/{filename}.json})
	 * <li>Other: {@code ~/.codion/{filename}.json}
	 * </ul>
	 * @param filename the filename
	 * @return the platform-specific preferences path
	 */
	static Path userPreferencesPath(String filename) {
		validateFilename(filename);
		if (PreferencesLocationHolder.LOCATION != null) {
			return Paths.get(PreferencesLocationHolder.LOCATION, filename + JSON);
		}
		String osName = System.getProperty("os.name", "").toLowerCase();
		if (osName.contains("win")) {
			return windowsPath(filename + JSON);
		}
		else if (osName.contains("mac")) {
			return macOSPath(filename + JSON);
		}
		else if (osName.contains("nix") || osName.contains("nux") || osName.contains("bsd")) {
			return linuxPath(filename + JSON);
		}
		else {
			return defaultPath(filename + JSON);
		}
	}

	private static Path windowsPath(String filename) {
		String localAppData = System.getenv("LOCALAPPDATA");
		if (localAppData != null) {
			return Paths.get(localAppData, CODION_DIR, filename);
		}

		String appData = System.getenv("APPDATA");
		if (appData != null) {
			return Paths.get(appData, CODION_DIR, filename);
		}

		return defaultPath(filename);
	}

	private static Path macOSPath(String filename) {
		String home = System.getProperty(USER_HOME);

		return Paths.get(home, "Library", "Preferences", CODION_DIR, filename);
	}

	private static Path linuxPath(String filename) {
		String xdgConfigHome = System.getenv("XDG_CONFIG_HOME");
		if (xdgConfigHome != null) {
			return Paths.get(xdgConfigHome, CODION_DIR.toLowerCase(), filename);
		}

		String home = System.getProperty(USER_HOME);

		return Paths.get(home, ".config", CODION_DIR.toLowerCase(), filename);
	}

	private static Path defaultPath(String filename) {
		String home = System.getProperty(USER_HOME);

		return Paths.get(home, "." + CODION_DIR.toLowerCase(), filename);
	}

	private static void validateFilename(String filename) {
		if (requireNonNull(filename).trim().isEmpty()) {
			throw new IllegalArgumentException("Filename must not be empty");
		}
	}
}