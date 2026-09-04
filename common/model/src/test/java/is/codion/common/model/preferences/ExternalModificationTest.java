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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.model.preferences;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.atomic.AtomicLong;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Two instances sharing a file, the changes one of them has not yet saved must survive the other one saving.
 */
public final class ExternalModificationTest {

	private static final AtomicLong TOUCHED = new AtomicLong(System.currentTimeMillis());

	@TempDir
	Path tempDir;

	@Test
	void syncKeepsUnsavedChanges() throws Exception {
		Path file = tempDir.resolve("prefs.json");
		Preferences first = new FilePreferences(new JsonPreferencesStore(file));
		first.put("first", "1");
		first.flush();
		touch(file);
		JsonPreferencesStore secondStore = new JsonPreferencesStore(file);
		Preferences second = new FilePreferences(secondStore);
		second.put("second", "2");
		//modified externally while the second has unsaved changes
		first.put("first", "3");
		first.flush();
		touch(file);
		second.sync();
		assertEquals("3", second.get("first", null));
		assertEquals("2", second.get("second", null));
		assertEquals(1, secondStore.saves());
		touch(file);
		first.sync();
		assertEquals("2", first.get("second", null));
		assertEquals("3", first.get("first", null));
	}

	@Test
	void flushKeepsExternalChanges() throws Exception {
		Path file = tempDir.resolve("prefs.json");
		Preferences first = new FilePreferences(new JsonPreferencesStore(file));
		first.put("first", "1");
		first.flush();
		touch(file);
		Preferences second = new FilePreferences(new JsonPreferencesStore(file));
		second.put("second", "2");
		first.put("first", "3");
		first.flush();
		touch(file);
		second.flush();
		Preferences reloaded = new FilePreferences(new JsonPreferencesStore(file));
		assertEquals("3", reloaded.get("first", null));
		assertEquals("2", reloaded.get("second", null));
	}

	@Test
	void flushWithoutChangesLeavesFileUntouched() throws Exception {
		Path file = tempDir.resolve("prefs.json");
		Preferences first = new FilePreferences(new JsonPreferencesStore(file));
		first.put("first", "1");
		first.flush();
		touch(file);
		JsonPreferencesStore secondStore = new JsonPreferencesStore(file);
		Preferences second = new FilePreferences(secondStore);
		first.put("first", "3");
		first.flush();
		touch(file);
		second.flush();
		assertEquals(0, secondStore.saves());
		assertEquals("3", second.get("first", null));
		assertEquals("3", new FilePreferences(new JsonPreferencesStore(file)).get("first", null));
	}

	@Test
	void unsavedRemovalsKept() throws Exception {
		Path file = tempDir.resolve("prefs.json");
		Preferences first = new FilePreferences(new JsonPreferencesStore(file));
		first.put("first", "1");
		first.node("node").put("key", "1");
		first.flush();
		touch(file);
		Preferences second = new FilePreferences(new JsonPreferencesStore(file));
		second.remove("first");
		second.node("node").removeNode();
		first.put("third", "1");
		first.node("node").put("key", "2");
		first.flush();
		touch(file);
		second.sync();
		assertNull(second.get("first", null));
		assertFalse(second.nodeExists("node"));
		assertEquals("1", second.get("third", null));
		Preferences reloaded = new FilePreferences(new JsonPreferencesStore(file));
		assertNull(reloaded.get("first", null));
		assertFalse(reloaded.nodeExists("node"));
		assertEquals("1", reloaded.get("third", null));
	}

	@Test
	void savedChangesNotReapplied() throws Exception {
		Path file = tempDir.resolve("prefs.json");
		Preferences first = new FilePreferences(new JsonPreferencesStore(file));
		first.put("first", "1");
		first.flush();
		touch(file);
		Preferences second = new FilePreferences(new JsonPreferencesStore(file));
		second.put("second", "2");
		second.sync();
		touch(file);
		first.sync();
		assertEquals("2", first.get("second", null));
		first.put("second", "9");
		first.flush();
		touch(file);
		second.sync();
		assertEquals("9", second.get("second", null));
	}

	@Test
	void fileDeletedExternally() throws Exception {
		Path file = tempDir.resolve("prefs.json");
		Preferences first = new FilePreferences(new JsonPreferencesStore(file));
		first.put("first", "1");
		first.flush();
		Preferences second = new FilePreferences(new JsonPreferencesStore(file));
		assertEquals("1", second.get("first", null));
		Files.delete(file);
		second.put("second", "2");
		second.sync();
		assertNull(second.get("first", null));
		assertEquals("2", second.get("second", null));
		Preferences reloaded = new FilePreferences(new JsonPreferencesStore(file));
		assertNull(reloaded.get("first", null));
		assertEquals("2", reloaded.get("second", null));
	}

	/**
	 * The modification time is coarse grained, a save following another one within the same clock tick would go unnoticed,
	 * so each save is stamped with a strictly increasing time.
	 */
	private static void touch(Path file) throws IOException {
		Files.setLastModifiedTime(file, FileTime.fromMillis(TOUCHED.addAndGet(1000)));
	}
}
