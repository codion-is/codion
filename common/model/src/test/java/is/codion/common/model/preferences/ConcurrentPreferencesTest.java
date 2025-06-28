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

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public final class ConcurrentPreferencesTest {

	@TempDir
	Path tempDir;

	@Test
	void testConcurrentAccess() throws Exception {
		Path prefsPath = tempDir.resolve("concurrent-test.json");
		int numThreads = 10;
		int numOperations = 100;

		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch endLatch = new CountDownLatch(numThreads);
		List<Future<Void>> futures = new ArrayList<>();

		// Create multiple threads that read and write concurrently
		for (int i = 0; i < numThreads; i++) {
			final int threadId = i;
			Future<Void> future = executor.submit(() -> {
				try {
					startLatch.await(); // Wait for all threads to be ready

					JsonPreferencesStore store = new JsonPreferencesStore(prefsPath);
					Random random = new Random(threadId);

					for (int op = 0; op < numOperations; op++) {
						String key = "key" + random.nextInt(20);
						String value = "thread" + threadId + "-value" + op;

						// Mix of operations
						switch (random.nextInt(4)) {
							case 0: // Write
								store.put("", key, value);
								break;
							case 1: // Read
								store.get("", key);
								break;
							case 2: // Save
								store.save();
								break;
							case 3: // Reload
								store.reload();
								break;
						}
					}

					// Final save
					store.save();
					return null;
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
				finally {
					endLatch.countDown();
				}
			});
			futures.add(future);
		}

		// Start all threads simultaneously
		startLatch.countDown();

		// Wait for completion
		assertTrue(endLatch.await(30, TimeUnit.SECONDS), "Threads did not complete in time");

		// Check for exceptions
		for (Future<Void> future : futures) {
			future.get(); // This will throw if any thread had an exception
		}

		executor.shutdown();

		// Verify file exists and is valid JSON
		assertTrue(Files.exists(prefsPath));
		String content = Files.readString(prefsPath);
		assertDoesNotThrow(() -> new org.json.JSONObject(content));
	}

	@Test
	void testMultiJVMSimulation() throws Exception {
		Path prefsPath = tempDir.resolve("multi-jvm-test.json");

		// Simulate two JVMs by creating two separate stores
		JsonPreferencesStore store1 = new JsonPreferencesStore(prefsPath);
		JsonPreferencesStore store2 = new JsonPreferencesStore(prefsPath);

		// JVM 1 writes
		store1.put("", "jvm1.key", "jvm1.value");
		store1.save();

		// Small delay to ensure file system timestamps are different
		Thread.sleep(10);

		// JVM 2 should see the change after reload
		store2.reload();
		assertEquals("jvm1.value", store2.get("", "jvm1.key"));

		// JVM 2 writes
		store2.put("", "jvm2.key", "jvm2.value");
		store2.save();

		// Small delay to ensure file system timestamps are different
		Thread.sleep(10);

		// JVM 1 should see the change after reload
		store1.reload();
		assertEquals("jvm2.value", store1.get("", "jvm2.key"));

		// Both should have both values
		assertEquals("jvm1.value", store1.get("", "jvm1.key"));
		assertEquals("jvm1.value", store2.get("", "jvm1.key"));
		assertEquals("jvm2.value", store1.get("", "jvm2.key"));
		assertEquals("jvm2.value", store2.get("", "jvm2.key"));
	}

	@Test
	void testAtomicWrites() throws Exception {
		Path prefsPath = tempDir.resolve("atomic-test.json");
		JsonPreferencesStore store = new JsonPreferencesStore(prefsPath);

		// Write initial data
		for (int i = 0; i < 100; i++) {
			store.put("", "key" + i, "value" + i);
		}
		store.save();

		// Start a thread that continuously reads
		AtomicBoolean corrupted = new AtomicBoolean(false);
		AtomicBoolean stopReading = new AtomicBoolean(false);

		Thread reader = new Thread(() -> {
			while (!stopReading.get()) {
				try {
					if (Files.exists(prefsPath)) {
						String content = Files.readString(prefsPath);
						// Try to parse - should never fail if writes are atomic
						new org.json.JSONObject(content);
					}
				}
				catch (Exception e) {
					corrupted.set(true);
					break;
				}
			}
		});
		reader.start();

		// Continuously write for a bit
		for (int i = 0; i < 50; i++) {
			store.put("", "key" + i, "updated" + i);
			store.save();
			Thread.sleep(10); // Small delay
		}

		// Stop reader
		stopReading.set(true);
		reader.join();

		// Should never have seen corrupted data
		assertFalse(corrupted.get(), "Reader saw corrupted data - writes not atomic");
	}

	@Test
	void testLockTimeout() throws Exception {
		Path prefsPath = tempDir.resolve("lock-timeout-test.json");
		Path lockPath = prefsPath.resolveSibling(prefsPath.getFileName() + ".lock");

		// Create a lock file that won't be released
		Files.createDirectories(lockPath.getParent());
		try (RandomAccessFile lockFile = new RandomAccessFile(lockPath.toFile(), "rw");
				 FileChannel channel = lockFile.getChannel();
				 FileLock lock = channel.lock()) {

			// Try to create store - should timeout
			JsonPreferencesStore store = new JsonPreferencesStore(prefsPath);

			// Try to save - should timeout
			store.put("", "key", "value");
			assertThrows(IOException.class, () -> store.save());
		}
	}

	@Test
	void testSyncBehavior() throws Exception {
		Path prefsPath = tempDir.resolve("sync-test.json");

		// Create preferences using the high-level API
		JsonPreferencesStore store = new JsonPreferencesStore(prefsPath);

		// Create another instance for the preferences API
		JsonPreferencesStore prefsStore = new JsonPreferencesStore(prefsPath);
		FilePreferences prefs = new FilePreferences(prefsStore);

		// Write through preferences API
		prefs.put("api.key", "api.value");
		prefs.flush();

		// Store should see it after reload
		store.reload();
		assertEquals("api.value", store.get("", "api.key"));

		// Write through store
		store.put("", "store.key", "store.value");
		store.save();

		// Preferences should see it after sync
		prefs.sync();
		assertEquals("store.value", prefs.get("store.key", null));
	}
}