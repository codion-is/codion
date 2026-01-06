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

import is.codion.common.utilities.Text;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

public final class FilePreferencesTest {

	@TempDir
	Path tempDir;

	private Preferences createTestPreferences() throws IOException {
		Path testFile = tempDir.resolve("test-" + System.nanoTime() + ".json");
		JsonPreferencesStore store = new JsonPreferencesStore(testFile);

		return new FilePreferences(store);
	}

	@Test
	void testLargeKeys() throws Exception {
		Preferences prefs = createTestPreferences();

		// Test key larger than 80 chars (the default limit)
		String longKey = "this.is.a.very.long.key.that.exceeds.the.default.limit.of.eighty.characters.maximum.length.and.should.work.fine";
		prefs.put(longKey, "value");

		assertEquals("value", prefs.get(longKey, null));

		// Test persistence
		prefs.flush();
		prefs.sync();
		assertEquals("value", prefs.get(longKey, null));
	}

	@Test
	void testLargeValues() throws Exception {
		Preferences prefs = createTestPreferences();

		// Test value larger than 8KB (the default limit)
		String hugeValue = Text.leftPad("", 100_000, 'x'); // 100KB
		prefs.put("key", hugeValue);

		assertEquals(hugeValue, prefs.get("key", null));

		// Test persistence
		prefs.flush();
		prefs.sync();
		assertEquals(hugeValue, prefs.get("key", null));
	}

	@Test
	void testBasicOperations() throws Exception {
		Preferences prefs = createTestPreferences();

		// Put
		prefs.put("test.key", "test.value");
		prefs.put("another.key", "another.value");

		// Get
		assertEquals("test.value", prefs.get("test.key", null));
		assertEquals("default", prefs.get("missing.key", "default"));

		// Keys
		String[] keys = prefs.keys();
		assertEquals(2, keys.length);
		assertTrue(containsKey(keys, "test.key"));
		assertTrue(containsKey(keys, "another.key"));

		// Remove
		prefs.remove("test.key");
		assertNull(prefs.get("test.key", null));

		// Verify removal persists
		prefs.flush();
		prefs.sync();
		assertNull(prefs.get("test.key", null));
		assertEquals("another.value", prefs.get("another.key", null));
	}

	@Test
	void testSpecialCharacters() throws Exception {
		Preferences prefs = createTestPreferences();

		// Test newlines, tabs, and other special characters
		String multilineValue = "Line 1\nLine 2\tTabbed\r\nLine 3";
		prefs.put("multiline.key", multilineValue);

		assertEquals(multilineValue, prefs.get("multiline.key", null));

		// Test persistence
		prefs.flush();
		prefs.sync();
		assertEquals(multilineValue, prefs.get("multiline.key", null));
	}

	@Test
	void testEmptyValue() throws Exception {
		Preferences prefs = createTestPreferences();

		prefs.put("empty.key", "");
		assertEquals("", prefs.get("empty.key", null));

		prefs.flush();
		prefs.sync();
		assertEquals("", prefs.get("empty.key", null));
	}

	@Test
	void testEmptyPreferencesNotWritten() throws Exception {
		Path testFile = tempDir.resolve("empty-test-" + System.nanoTime() + ".json");
		JsonPreferencesStore store = new JsonPreferencesStore(testFile);
		Preferences prefs = new FilePreferences(store);

		// Flush empty preferences - file should not be created
		prefs.flush();
		assertFalse(testFile.toFile().exists(), "Empty preferences should not create file");

		// Add a value, flush - file should be created
		prefs.put("key", "value");
		prefs.flush();
		assertTrue(testFile.toFile().exists(), "Non-empty preferences should create file");

		// Remove all values, flush - file should be deleted
		prefs.remove("key");
		prefs.flush();
		assertFalse(testFile.toFile().exists(), "Empty preferences should delete existing file");
	}

	private static boolean containsKey(String[] keys, String key) {
		for (String k : keys) {
			if (k.equals(key)) {
				return true;
			}
		}

		return false;
	}
}