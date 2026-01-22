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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

public final class FilePreferencesHierarchyTest {

	@TempDir
	Path tempDir;

	private Preferences createTestPreferences() {
		Path testFile = tempDir.resolve("test-" + System.nanoTime() + ".json");
		JsonPreferencesStore store = new JsonPreferencesStore(testFile);

		return new FilePreferences(store);
	}

	@Test
	void testBasicHierarchy() throws Exception {
		Preferences root = createTestPreferences();

		// Create child nodes
		Preferences child1 = root.node("child1");
		Preferences child2 = root.node("child2");

		// Put values in different levels
		root.put("root.key", "root.value");
		child1.put("child1.key", "child1.value");
		child2.put("child2.key", "child2.value");

		// Verify values
		assertEquals("root.value", root.get("root.key", null));
		assertEquals("child1.value", child1.get("child1.key", null));
		assertEquals("child2.value", child2.get("child2.key", null));

		// Verify isolation between nodes
		assertNull(root.get("child1.key", null));
		assertNull(child1.get("root.key", null));
		assertNull(child1.get("child2.key", null));

		// Verify children names
		String[] childrenNames = root.childrenNames();
		assertEquals(2, childrenNames.length);
		assertTrue(Arrays.asList(childrenNames).contains("child1"));
		assertTrue(Arrays.asList(childrenNames).contains("child2"));

		// Persist and reload
		root.flush();
		root.sync();

		// Verify persistence
		assertEquals("root.value", root.get("root.key", null));
		assertEquals("child1.value", child1.get("child1.key", null));
		assertEquals("child2.value", child2.get("child2.key", null));
	}

	@Test
	void testDeepHierarchy() throws Exception {
		Preferences root = createTestPreferences();

		// Create deep hierarchy
		Preferences level1 = root.node("level1");
		Preferences level2 = level1.node("level2");
		Preferences level3 = level2.node("level3");

		// Put values at each level
		root.put("key", "root");
		level1.put("key", "level1");
		level2.put("key", "level2");
		level3.put("key", "level3");

		// Verify each level has its own value
		assertEquals("root", root.get("key", null));
		assertEquals("level1", level1.get("key", null));
		assertEquals("level2", level2.get("key", null));
		assertEquals("level3", level3.get("key", null));

		// Test navigation
		assertEquals("level3", root.node("level1/level2/level3").get("key", null));
		assertEquals("level2", root.node("level1/level2").get("key", null));

		// Persist and verify
		root.flush();
		root.sync();
		assertEquals("level3", root.node("level1/level2/level3").get("key", null));
	}

	@Test
	void testNodeRemoval() throws Exception {
		Preferences root = createTestPreferences();

		// Create nodes with values
		Preferences child = root.node("child");
		Preferences grandchild = child.node("grandchild");

		child.put("key", "value");
		grandchild.put("key", "value");

		// Remove child node
		child.removeNode();
		root.flush();

		// Verify child and its descendants are removed
		assertFalse(root.nodeExists("child"));
		assertFalse(root.nodeExists("child/grandchild"));

		// Verify root is unaffected
		assertTrue(root.nodeExists(""));
	}

	@Test
	void testLargeKeysAndValuesWithHierarchy() throws Exception {
		Preferences root = createTestPreferences();
		Preferences child = root.node("config").node("application").node("settings");

		// Large key in nested node
		String longKey = "this.is.a.very.long.key.that.exceeds.the.default.limit.of.eighty.characters.in.nested.preferences.node";
		String largeValue = Text.leftPad("", 50_000, 'x'); // 50KB

		child.put(longKey, largeValue);

		// Verify through hierarchy
		assertEquals(largeValue, child.get(longKey, null));
		assertEquals(largeValue, root.node("config/application/settings").get(longKey, null));

		// Persist and reload
		root.flush();
		root.sync();
		assertEquals(largeValue, child.get(longKey, null));
	}

	@Test
	void testNodeWithBothValuesAndChildren() throws Exception {
		Preferences root = createTestPreferences();
		Preferences parent = root.node("parent");

		// Add both values and children to the same node
		parent.put("parent.key", "parent.value");
		Preferences child = parent.node("child");
		child.put("child.key", "child.value");

		// Verify both coexist
		assertEquals("parent.value", parent.get("parent.key", null));
		assertEquals("child.value", child.get("child.key", null));

		// Verify keys() only returns value keys, not child nodes
		String[] parentKeys = parent.keys();
		assertEquals(1, parentKeys.length);
		assertEquals("parent.key", parentKeys[0]);

		// Verify childrenNames() only returns child nodes
		String[] childrenNames = parent.childrenNames();
		assertEquals(1, childrenNames.length);
		assertEquals("child", childrenNames[0]);
	}

	@Test
	void testJsonStructure() throws Exception {
		Path testFile = tempDir.resolve("hierarchy-test.json");
		JsonPreferencesStore store = new JsonPreferencesStore(testFile);
		Preferences root = new FilePreferences(store);

		// Create structure
		root.put("root.setting", "value1");
		Preferences app = root.node("application");
		app.put("app.name", "MyApp");
		app.put("app.version", "1.0");
		Preferences ui = app.node("ui");
		ui.put("theme", "dark");
		ui.put("font.size", "12");

		root.flush();

		// Verify JSON structure
		String json = new String(Files.readAllBytes(testFile), StandardCharsets.UTF_8);
		assertTrue(json.contains("\"root.setting\": \"value1\""));
		assertTrue(json.contains("\"application\": {"));
		assertTrue(json.contains("\"app.name\": \"MyApp\""));
		assertTrue(json.contains("\"ui\": {"));
		assertTrue(json.contains("\"theme\": \"dark\""));
	}

	@Test
	void testNodeExistsAndNodePath() throws Exception {
		Preferences root = createTestPreferences();

		assertFalse(root.nodeExists("nonexistent"));

		Preferences child = root.node("existing");
		child.put("key", "value");

		assertTrue(root.nodeExists("existing"));
		assertTrue(root.nodeExists("/existing"));

		// Test nested paths
		Preferences nested = child.node("nested");
		assertTrue(root.nodeExists("existing/nested"));
		assertTrue(root.nodeExists("/existing/nested"));

		// Test absolutePath
		assertEquals("/", root.absolutePath());
		assertEquals("/existing", child.absolutePath());
		assertEquals("/existing/nested", nested.absolutePath());
	}
}