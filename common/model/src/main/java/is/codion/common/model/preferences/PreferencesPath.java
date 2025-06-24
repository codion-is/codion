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

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Provides platform-specific paths for storing preferences.
 */
final class PreferencesPath {

	private static final String PREFERENCES_FILENAME = "preferences.json";
	private static final String CODION_DIR = "Codion";
	private static final String CODION_DIR_LOWERCASE = "codion";
	private static final String USER_HOME = "user.home";

	private PreferencesPath() {}

	/**
	 * Returns the platform-specific path for storing user preferences.
	 *
	 * <ul>
	 * <li>Windows: {@code %LOCALAPPDATA%/Codion/preferences.json}
	 * <li>macOS: {@code ~/Library/Preferences/Codion/preferences.json}
	 * <li>Linux: {@code ~/.config/codion/preferences.json} (or {@code $XDG_CONFIG_HOME/codion/preferences.json})
	 * <li>Other: {@code ~/.codion/preferences.json}
	 * </ul>
	 * @return the platform-specific preferences path
	 */
	static Path userPreferencesPath() {
		String osName = System.getProperty("os.name", "").toLowerCase();
		if (osName.contains("win")) {
			return windowsPath();
		}
		else if (osName.contains("mac")) {
			return macOSPath();
		}
		else if (osName.contains("nix") || osName.contains("nux") || osName.contains("bsd")) {
			return linuxPath();
		}
		else {
			return defaultPath();
		}
	}

	private static Path windowsPath() {
		String localAppData = System.getenv("LOCALAPPDATA");
		if (localAppData != null) {
			return Paths.get(localAppData, CODION_DIR, PREFERENCES_FILENAME);
		}

		String appData = System.getenv("APPDATA");
		if (appData != null) {
			return Paths.get(appData, CODION_DIR, PREFERENCES_FILENAME);
		}

		return defaultPath();
	}

	private static Path macOSPath() {
		String home = System.getProperty(USER_HOME);

		return Paths.get(home, "Library", "Preferences", CODION_DIR, PREFERENCES_FILENAME);
	}

	private static Path linuxPath() {
		String xdgConfigHome = System.getenv("XDG_CONFIG_HOME");
		if (xdgConfigHome != null) {
			return Paths.get(xdgConfigHome, CODION_DIR_LOWERCASE, PREFERENCES_FILENAME);
		}

		String home = System.getProperty(USER_HOME);

		return Paths.get(home, ".config", CODION_DIR_LOWERCASE, PREFERENCES_FILENAME);
	}

	private static Path defaultPath() {
		String home = System.getProperty(USER_HOME);

		return Paths.get(home, "." + CODION_DIR_LOWERCASE, PREFERENCES_FILENAME);
	}
}