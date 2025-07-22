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
 * Copyright (c) 2016 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.model.preferences;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static java.util.Objects.requireNonNull;

/**
 * A utility class for working with user preferences
 */
public final class UserPreferences {

	private static final Map<String, FilePreferences> FILE_PREFERENCES = new ConcurrentHashMap<>();

	private static @Nullable Preferences preferences;

	private UserPreferences() {}

	/**
	 * @param key the key identifying the preference
	 * @return the user preference associated with the given key
	 */
	public static @Nullable String get(String key) {
		return userPreferences().get(requireNonNull(key), null);
	}

	/**
	 * @param key the key identifying the preference
	 * @param defaultValue the default value if no preference is available
	 * @return the user preference associated with the given key
	 * @throws NullPointerException in case {@code defaultValue} is null
	 */
	public static String get(String key, String defaultValue) {
		return userPreferences().get(requireNonNull(key), requireNonNull(defaultValue));
	}

	/**
	 * @param key the key to use to identify the preference
	 * @param value the preference value to associate with the given key
	 */
	public static void set(String key, String value) {
		userPreferences().put(requireNonNull(key), value);
	}

	/**
	 * Removes the preference associated with the given key
	 * @param key the key to use to identify the preference to remove
	 */
	public static void remove(String key) {
		userPreferences().remove(requireNonNull(key));
	}

	/**
	 * Flushes the preferences to disk, including all file based preferences initialized via {@link #file(String)}.
	 * @throws BackingStoreException in case of a backing store failure
	 */
	public static void flush() throws BackingStoreException {
		userPreferences().flush();
		for (Preferences preferences : FILE_PREFERENCES.values()) {
			preferences.flush();
		}
	}

	/**
	 * @param filename the preferences filename
	 * @return a file based Preferences instance using the given filename
	 */
	public static Preferences file(String filename) {
		validateFilename(filename);
		return FILE_PREFERENCES.computeIfAbsent(filename, k -> {
			try {
				return new FilePreferences(filename);
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	/**
	 * Deletes the preferences file with the given name.
	 * @param filename the name of the preferences file to delete
	 * @throws IOException in case of an exception
	 * @throws IllegalArgumentException in case the given preferences file does not exist
	 */
	public static void delete(String filename) throws IOException {
		validateFilename(filename);
		FilePreferences filePreferences = FILE_PREFERENCES.remove(filename);
		if (filePreferences == null) {
			throw new IllegalArgumentException("Preferences file with name '" +  filename + "' not found");
		}
		filePreferences.delete();
	}

	private static synchronized Preferences userPreferences() {
		if (preferences == null) {
			preferences = Preferences.userRoot();
		}

		return preferences;
	}

	private static void validateFilename(String filename) {
		if (requireNonNull(filename).trim().isEmpty()) {
			throw new IllegalArgumentException("Filename must not be empty");
		}
	}
}
