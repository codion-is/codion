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

import is.codion.common.utilities.Text;

import java.util.prefs.Preferences;

/**
 * Example demonstrating the file-based preferences implementation.
 */
public final class FilePreferencesExample {

	public static void main(String[] args) throws Exception {
		// Enable file-based preferences
		System.setProperty("java.util.prefs.PreferencesFactory",
						"is.codion.common.model.preferences.FilePreferencesFactory");

		Preferences prefs = Preferences.userRoot();

		// Store large keys (> 80 chars)
		String longKey = "this.is.a.very.long.key.that.exceeds.the.default.limit.of.eighty.characters.maximum.length.and.should.work.fine";
		prefs.put(longKey, "value for long key");

		// Store large values (> 8KB)
		String hugeValue = Text.leftPad("", 100_000, 'x'); // 100KB
		prefs.put("huge.value", hugeValue);

		prefs.put("json.data", "{\"name\": \"a name\"}");

		// Persist to disk
		prefs.flush();

		System.out.println("Preferences stored successfully!");
		System.out.println("Check ~/.codion/preferences.json to see the stored data");

		// Read back
		System.out.println("\nReading back:");
		System.out.println("Long key value: " + prefs.get(longKey, null));
		System.out.println("Huge value length: " + prefs.get("huge.value", "").length());
		System.out.println("JSON data: " + prefs.get("json.data", null));
	}
}