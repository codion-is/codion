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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

		// what the store holds is kept, it being the last good copy
		assertEquals("value", store.get("", "key"));
	}

	@Test
	void corruptAtReloadKeepsContents() throws Exception {
		// a file found corrupt at a reload replaced what the store held, the last good copy,
		// so that the next save wrote only the changes made since
		Path prefsFile = tempDir.resolve("keep.json");
		JsonPreferencesStore store = new JsonPreferencesStore(prefsFile);
		store.put("", "one", "1");
		store.put("node", "two", "2");
		store.sync();
		Thread.sleep(10);
		Files.write(prefsFile.toAbsolutePath(), "{\"one\":\"1\",\"no".getBytes(StandardCharsets.UTF_8));

		store.put("", "three", "3");
		store.sync();

		assertEquals("1", store.get("", "one"));
		JsonPreferencesStore restarted = new JsonPreferencesStore(prefsFile);
		assertEquals("1", restarted.get("", "one"));
		assertEquals("2", restarted.get("node", "two"));
		assertEquals("3", restarted.get("", "three"));
		assertEquals(1, backups("keep.json"));
	}

	@Test
	void corruptFileBackedUpOnce() throws Exception {
		// a corrupt file was backed up again at every flush, and left corrupt as long as there was nothing to save
		Path prefsFile = tempDir.resolve("once.json");
		JsonPreferencesStore store = new JsonPreferencesStore(prefsFile);
		store.put("", "one", "1");
		store.sync();
		Thread.sleep(10);
		Files.write(prefsFile.toAbsolutePath(), "{\"one\":\"1\",\"no".getBytes(StandardCharsets.UTF_8));

		for (int i = 0; i < 5; i++) {
			store.sync();
			Thread.sleep(2);
		}

		assertEquals(1, backups("once.json"));
		assertEquals("1", new JsonPreferencesStore(prefsFile).get("", "one"));
		assertEquals(1, backups("once.json"));

		// found corrupt at start, backed up then and not again at the first flush
		Path startFile = tempDir.resolve("start.json");
		Files.write(startFile.toAbsolutePath(), "{\"one\":\"1\",\"no".getBytes(StandardCharsets.UTF_8));
		JsonPreferencesStore started = new JsonPreferencesStore(startFile);
		assertNull(started.get("", "one"));
		assertEquals(1, backups("start.json"));
		Thread.sleep(2);
		started.put("", "two", "2");
		started.sync();
		assertEquals(1, backups("start.json"));
		assertEquals("2", new JsonPreferencesStore(startFile).get("", "two"));
	}

	private long backups(String filename) throws IOException {
		try (Stream<Path> files = Files.list(tempDir)) {
			return files.filter(path -> path.getFileName().toString().startsWith(filename + ".corrupt.")).count();
		}
	}
}