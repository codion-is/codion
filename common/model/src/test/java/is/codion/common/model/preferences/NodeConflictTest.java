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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for node conflict scenarios in JsonPreferencesStore.
 */
public final class NodeConflictTest {

	@TempDir
	Path tempDir;

	@Test
	void testCreatingNodeWhereValueExists() {
		Path prefsFile = tempDir.resolve("conflict.json");
		JsonPreferencesStore store = new JsonPreferencesStore(prefsFile);

		// Set a value at "foo"
		store.put("", "foo", "value");

		// Try to create a child node "foo/bar" - should throw exception
		IllegalStateException exception = assertThrows(IllegalStateException.class,
						() -> store.put("foo", "bar", "childValue"));

		assertTrue(exception.getMessage().contains("Cannot create node"));
		assertTrue(exception.getMessage().contains("foo"));
		assertTrue(exception.getMessage().contains("value already exists"));

		// Original value should still be there
		assertEquals("value", store.get("", "foo"));
	}

	@Test
	void testCreatingNestedPathThroughValue() {
		Path prefsFile = tempDir.resolve("nested-conflict.json");
		JsonPreferencesStore store = new JsonPreferencesStore(prefsFile);

		// Create a hierarchy with a value in the middle
		store.put("app", "setting", "value1");

		// Try to create a deeper path through the value
		IllegalStateException exception = assertThrows(IllegalStateException.class,
						() -> store.put("app/setting/subsetting", "key", "value2"));

		assertTrue(exception.getMessage().contains("Cannot create node"));
		assertTrue(exception.getMessage().contains("setting"));

		// Original structure should be unchanged
		assertEquals("value1", store.get("app", "setting"));
	}

	@Test
	void testValidNodeCreation() {
		Path prefsFile = tempDir.resolve("valid-nodes.json");
		JsonPreferencesStore store = new JsonPreferencesStore(prefsFile);

		// Create a valid hierarchy
		store.put("app/ui/theme", "color", "dark");
		store.put("app/ui/theme", "font", "Arial");
		store.put("app/ui", "size", "large");
		store.put("app", "version", "1.0");

		// All values should be accessible
		assertEquals("dark", store.get("app/ui/theme", "color"));
		assertEquals("Arial", store.get("app/ui/theme", "font"));
		assertEquals("large", store.get("app/ui", "size"));
		assertEquals("1.0", store.get("app", "version"));

		// Verify hierarchy
		assertTrue(store.childrenNames("").contains("app"));
		assertTrue(store.childrenNames("app").contains("ui"));
		assertTrue(store.childrenNames("app/ui").contains("theme"));
	}

	@Test
	void testRemovingValueAllowsNodeCreation() {
		Path prefsFile = tempDir.resolve("remove-then-create.json");
		JsonPreferencesStore store = new JsonPreferencesStore(prefsFile);

		// Set a value
		store.put("", "foo", "value");
		assertEquals("value", store.get("", "foo"));

		// Remove the value
		store.remove("", "foo");
		assertNull(store.get("", "foo"));

		// Now we should be able to create a child node
		store.put("foo", "bar", "childValue");
		assertEquals("childValue", store.get("foo", "bar"));
	}

	@Test
	void testMultipleLevelConflicts() {
		Path prefsFile = tempDir.resolve("multi-level.json");
		JsonPreferencesStore store = new JsonPreferencesStore(prefsFile);

		// Create values at different levels
		store.put("level1", "value1", "data1");
		store.put("level1/level2", "value2", "data2");

		// Try to create conflicting paths
		assertThrows(IllegalStateException.class,
						() -> store.put("level1/value1", "key", "value"));

		assertThrows(IllegalStateException.class,
						() -> store.put("level1/level2/value2", "key", "value"));

		// Original values should be preserved
		assertEquals("data1", store.get("level1", "value1"));
		assertEquals("data2", store.get("level1/level2", "value2"));
	}

	@Test
	void testSaveAndReloadPreservesStructure() throws IOException {
		Path prefsFile = tempDir.resolve("save-reload.json");
		JsonPreferencesStore store = new JsonPreferencesStore(prefsFile);

		// Create a complex structure
		store.put("app/database", "host", "localhost");
		store.put("app/database", "port", "5432");
		store.put("app/ui/theme", "mode", "dark");
		store.put("app", "name", "MyApp");

		// Save
		store.save();

		// Create new store instance
		JsonPreferencesStore store2 = new JsonPreferencesStore(prefsFile);

		// Verify all data is preserved
		assertEquals("localhost", store2.get("app/database", "host"));
		assertEquals("5432", store2.get("app/database", "port"));
		assertEquals("dark", store2.get("app/ui/theme", "mode"));
		assertEquals("MyApp", store2.get("app", "name"));

		// Verify we still can't create conflicts
		assertThrows(IllegalStateException.class,
						() -> store2.put("app/name", "key", "value"));
	}
}