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

import java.nio.file.Path;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

public final class FilePreferencesTest {

	@TempDir
	Path tempDir;

	private Preferences createTestPreferences() {
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
	void valuesReadBackAsPut() throws Exception {
		// a value starting with a bracket was parsed leniently and stored as JSON, what came back being
		// that JSON written out again, and one holding an empty object lost it when saved
		String[] values = {
						"[a, b, c]", "[1, 2, 3]", "[x]", "{'a':'b'}", "{a=1, b=2}", "[abc",
						"{\"b\":1,\"a\":2}", "[1.10, 1e3, 007]", "  [1]  ", "{\"a\":1} trailing", "[1] [2]",
						"{\"a\":{},\"b\":1}", "{\"a\":{}}", "{\"a\":[]}", "[]", "[{}]", "{\"k\":null}",
						"{\".node\":true,\"a\":1}", "{\"w\":100,\"i\":0}", "[\"björn\"]"
		};
		Path testFile = tempDir.resolve("values-" + System.nanoTime() + ".json");
		Preferences prefs = new FilePreferences(new JsonPreferencesStore(testFile));
		Preferences node = prefs.node("node");
		for (int i = 0; i < values.length; i++) {
			node.put("key" + i, values[i]);
		}
		for (int i = 0; i < values.length; i++) {
			assertEquals(values[i], node.get("key" + i, null));
		}
		assertEquals(values.length, node.keys().length);
		assertEquals(0, node.childrenNames().length);
		prefs.flush();

		Preferences restarted = new FilePreferences(new JsonPreferencesStore(testFile)).node("node");
		for (int i = 0; i < values.length; i++) {
			assertEquals(values[i], restarted.get("key" + i, null));
		}
		assertEquals(values.length, restarted.keys().length);
		assertEquals(0, restarted.childrenNames().length);
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

	@Test
	void flushSavesOnce() throws Exception {
		Path file = tempDir.resolve("flush.json");
		JsonPreferencesStore store = new JsonPreferencesStore(file);
		Preferences root = new FilePreferences(store);
		for (int i = 0; i < 50; i++) {
			root.node("entities").node("entity" + i).node("view").put("key", "value" + i);
		}
		root.flush();
		assertEquals(1, store.saves());
		//nothing to save
		root.node("entities").node("entity1").flush();
		assertEquals(1, store.saves());
		root.node("entities").node("entity1").put("key", "changed");
		root.node("entities").node("entity1").flush();
		assertEquals(2, store.saves());
		//the whole tree is persisted
		Preferences reloaded = new FilePreferences(new JsonPreferencesStore(file));
		assertEquals("value49", reloaded.node("entities").node("entity49").node("view").get("key", null));
	}

	@Test
	void removeNodePersistedByFlush() throws Exception {
		Path file = tempDir.resolve("remove.json");
		JsonPreferencesStore store = new JsonPreferencesStore(file);
		Preferences root = new FilePreferences(store);
		root.put("key", "value");
		Preferences child = root.node("child");
		child.node("grandchild").put("key", "value");
		root.flush();
		assertEquals(1, store.saves());
		child.removeNode();
		assertEquals(1, store.saves());
		assertTrue(new FilePreferences(new JsonPreferencesStore(file)).nodeExists("child"));
		root.flush();
		assertEquals(2, store.saves());
		assertFalse(new FilePreferences(new JsonPreferencesStore(file)).nodeExists("child"));
	}
}