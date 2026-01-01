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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for corrupted JSON file handling in JsonPreferencesStore.
 */
public final class CorruptedFileTest {

	@TempDir
	Path tempDir;

	@Test
	void testMalformedJsonCreatesBackup() throws Exception {
		Path prefsFile = tempDir.resolve("prefs.json");

		// Create a malformed JSON file
		Files.write(prefsFile.toAbsolutePath(), "{ \"key\": \"value\" invalid json".getBytes());

		// Create store - should handle corruption
		JsonPreferencesStore store = new JsonPreferencesStore(prefsFile);

		// Verify backup was created
		List<Path> backupFiles = Files.list(tempDir)
						.filter(p -> p.getFileName().toString().startsWith("prefs.json.corrupt."))
						.collect(Collectors.toList());

		assertEquals(1, backupFiles.size(), "Should create exactly one backup file");

		// Verify backup contains original corrupted content
		String backupContent = new String(Files.readAllBytes(backupFiles.get(0)), StandardCharsets.UTF_8);
		assertEquals("{ \"key\": \"value\" invalid json", backupContent);

		// Verify store works with empty data
		assertNull(store.get("", "key"));

		// Verify we can save new data
		store.put("", "newKey", "newValue");
		store.save();

		// Verify saved file is valid JSON
		String savedContent = new String(Files.readAllBytes(prefsFile), StandardCharsets.UTF_8);
		assertTrue(savedContent.contains("\"newKey\""));
		assertTrue(savedContent.contains("\"newValue\""));
	}

	@Test
	void testEmptyFileHandling() throws Exception {
		Path prefsFile = tempDir.resolve("empty.json");

		// Create empty file
		Files.write(prefsFile.toAbsolutePath(), "".getBytes());

		// Create store - should handle empty file as corrupted
		JsonPreferencesStore store = new JsonPreferencesStore(prefsFile);

		// Verify backup was created
		List<Path> backupFiles = Files.list(tempDir)
						.filter(p -> p.getFileName().toString().startsWith("empty.json.corrupt."))
						.collect(Collectors.toList());

		assertEquals(1, backupFiles.size(), "Should create backup for empty file");

		// Verify store works
		store.put("", "key", "value");
		store.save();

		assertTrue(new String(Files.readAllBytes(prefsFile), StandardCharsets.UTF_8).contains("\"key\""));
	}

	@Test
	void testBinaryDataCreatesBackup() throws Exception {
		Path prefsFile = tempDir.resolve("binary.json");

		// Write binary data
		Files.write(prefsFile, new byte[] {0x00, 0x01, 0x02, 0x03, (byte) 0xFF});

		// Create store
		JsonPreferencesStore store = new JsonPreferencesStore(prefsFile);

		// Verify backup was created
		List<Path> backupFiles = Files.list(tempDir)
						.filter(p -> p.getFileName().toString().startsWith("binary.json.corrupt."))
						.collect(Collectors.toList());

		assertEquals(1, backupFiles.size(), "Should create backup for binary file");

		// Verify store works with empty data
		assertTrue(store.keys("").isEmpty());
	}

	@Test
	void testPartiallyCorruptedJson() throws Exception {
		Path prefsFile = tempDir.resolve("partial.json");

		// Create JSON that starts valid but is truncated
		Files.write(prefsFile.toAbsolutePath(), "{ \"key1\": \"value1\", \"key2\": \"val".getBytes());

		// Create store
		JsonPreferencesStore store = new JsonPreferencesStore(prefsFile);

		// Verify backup was created
		List<Path> backupFiles = Files.list(tempDir)
						.filter(p -> p.getFileName().toString().startsWith("partial.json.corrupt."))
						.collect(Collectors.toList());

		assertEquals(1, backupFiles.size(), "Should create backup for truncated JSON");

		// Store should be empty after corruption
		assertNull(store.get("", "key1"));
		assertNull(store.get("", "key2"));
	}

	@Test
	void testValidJsonNotBackedUp() throws Exception {
		Path prefsFile = tempDir.resolve("valid.json");

		// Create valid JSON
		Files.write(prefsFile.toAbsolutePath(), "{ \"key\": \"value\" }".getBytes());

		// Create store
		JsonPreferencesStore store = new JsonPreferencesStore(prefsFile);

		// Verify NO backup was created
		List<Path> backupFiles = Files.list(tempDir)
						.filter(p -> p.getFileName().toString().contains(".corrupt."))
						.collect(Collectors.toList());

		assertEquals(0, backupFiles.size(), "Should not create backup for valid JSON");

		// Verify data was loaded
		assertEquals("value", store.get("", "key"));
	}

	@Test
	void testReloadWithCorruption() throws Exception {
		Path prefsFile = tempDir.resolve("reload.json");

		// Start with valid JSON
		Files.write(prefsFile.toAbsolutePath(), "{ \"key\": \"value\" }".getBytes());
		JsonPreferencesStore store = new JsonPreferencesStore(prefsFile);
		assertEquals("value", store.get("", "key"));

		// Small delay to ensure file modification time is different
		Thread.sleep(10);

		// Corrupt the file externally
		Files.write(prefsFile.toAbsolutePath(), "corrupted{".getBytes());

		// Reload should handle corruption
		store.reload();

		// Verify backup was created
		List<Path> backupFiles = Files.list(tempDir)
						.filter(p -> p.getFileName().toString().startsWith("reload.json.corrupt."))
						.collect(Collectors.toList());

		assertEquals(1, backupFiles.size(), "Should create backup on reload corruption");

		// Data should be cleared
		assertNull(store.get("", "key"));
	}
}