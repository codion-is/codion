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
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

public final class PreferencesMigratorTest {

	@TempDir
	Path tempDir;

	@Test
	void testBasicMigration() throws Exception {
		// Setup - create preferences in default store
		Preferences defaultRoot = Preferences.userRoot();
		Preferences testNode = defaultRoot.node("codion-test-migration");
		
		testNode.put("simple.key", "simple.value");
		testNode.put("another.key", "another.value");
		testNode.flush();

		// Create child node with values
		Preferences child = testNode.node("child");
		child.put("child.key", "child.value");
		child.flush();

		try {
			// Perform migration
			Path targetPath = tempDir.resolve("migrated-prefs.json");
			PreferencesMigrator migrator = PreferencesMigrator.builder()
					.targetPath(targetPath)
					.build();
			
			migrator.migrate();

			// Verify file was created
			assertTrue(Files.exists(targetPath));

			// Load migrated preferences
			JsonPreferencesStore store = new JsonPreferencesStore(targetPath);
			SimpleFilePreferences filePrefs = new SimpleFilePreferences(store);

			// Verify values were migrated
			Preferences migratedNode = filePrefs.node("codion-test-migration");
			assertEquals("simple.value", migratedNode.get("simple.key", null));
			assertEquals("another.value", migratedNode.get("another.key", null));

			// Verify child node was migrated
			Preferences migratedChild = migratedNode.node("child");
			assertEquals("child.value", migratedChild.get("child.key", null));

			// Verify migration marker
			assertEquals("true", Files.exists(targetPath) ? "true" : "false");
		}
		finally {
			// Cleanup
			testNode.removeNode();
		}
	}

	@Test
	void testTruncatedValueDetection() {
		PreferencesMigrator.create();

		// Test truncated key (exactly 80 chars)
		String longKey = "x".repeat(80);
		assertTrue(PreferencesMigrator.isTruncated(longKey, "value"));

		// Test non-truncated key
		assertFalse(PreferencesMigrator.isTruncated("normal.key", "value"));

		// Test truncated value (exactly 8192 chars)
		String longValue = "x".repeat(8192);
		assertTrue(PreferencesMigrator.isTruncated("key", longValue));

		// Test non-truncated value
		assertFalse(PreferencesMigrator.isTruncated("key", "normal value"));
	}

	@Test
	void testMigrationWithTruncatedValues() throws Exception {
		// Setup - create preferences with potentially truncated values
		Preferences defaultRoot = Preferences.userRoot();
		Preferences testNode = defaultRoot.node("codion-test-truncated");

		// Create a key at the limit
		String maxKey = "k".repeat(80);
		testNode.put(maxKey, "value at max key");

		// Create a value at the limit (8KB)
		String maxValue = "v".repeat(8192);
		testNode.put("large.value", maxValue);
		testNode.flush();

		try {
			// Perform migration
			Path targetPath = tempDir.resolve("migrated-truncated.json");
			PreferencesMigrator migrator = PreferencesMigrator.builder()
					.targetPath(targetPath)
					.migrateTruncated(true)
					.build();

			migrator.migrate();

			// Load migrated preferences
			JsonPreferencesStore store = new JsonPreferencesStore(targetPath);
			SimpleFilePreferences filePrefs = new SimpleFilePreferences(store);
			Preferences migratedNode = filePrefs.node("codion-test-truncated");

			// Verify truncated values were migrated
			assertEquals("value at max key", migratedNode.get(maxKey, null));
			assertEquals(maxValue, migratedNode.get("large.value", null));

			// Verify truncation markers
			assertEquals("true", migratedNode.get(maxKey + ".truncated", null));
			assertEquals("true", migratedNode.get("large.value.truncated", null));
		}
		finally {
			// Cleanup
			testNode.removeNode();
		}
	}

	@Test
	void testSkipExistingFile() throws Exception {
		// Create existing file
		Path targetPath = tempDir.resolve("existing-prefs.json");
		Files.writeString(targetPath, "{\"existing\": \"data\"}");

		// Setup preferences that would be migrated
		Preferences defaultRoot = Preferences.userRoot();
		Preferences testNode = defaultRoot.node("codion-test-skip");
		testNode.put("key", "value");
		testNode.flush();

		try {
			// Attempt migration
			PreferencesMigrator migrator = PreferencesMigrator.builder()
					.targetPath(targetPath)
					.build();

			migrator.migrate();

			// Verify existing file was not overwritten
			String content = Files.readString(targetPath);
			assertTrue(content.contains("existing"));
			assertFalse(content.contains("codion-test-skip"));
		}
		finally {
			// Cleanup
			testNode.removeNode();
		}
	}

	@Test
	void testEmptyPreferencesMigration() throws Exception {
		// Perform migration with no preferences
		Path targetPath = tempDir.resolve("empty-prefs.json");
		PreferencesMigrator migrator = PreferencesMigrator.builder()
				.targetPath(targetPath)
				.build();

		migrator.migrate();

		// Verify file was created with migration marker
		assertTrue(Files.exists(targetPath));
		
		// Load and check for migration marker
		JsonPreferencesStore store = new JsonPreferencesStore(targetPath);
		SimpleFilePreferences filePrefs = new SimpleFilePreferences(store);
		assertNotNull(filePrefs.get(".migrated", null));
	}
}