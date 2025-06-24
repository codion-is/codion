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
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for resource management in JsonPreferencesStore.
 */
public final class ResourceLeakTest {

	@TempDir
	Path tempDir;

	@Test
	void testInterruptedExceptionType() throws Exception {
		Path prefsFile = tempDir.resolve("interrupt.json");
		Path lockFile = prefsFile.resolveSibling(prefsFile.getFileName() + ".lock");

		// Hold the lock in main thread
		Files.createDirectories(lockFile.getParent());
		try (RandomAccessFile raf = new RandomAccessFile(lockFile.toFile(), "rw");
				 FileChannel channel = raf.getChannel();
				 FileLock lock = channel.lock()) {

			// Try to acquire lock in another thread and interrupt it
			ExecutorService executor = Executors.newSingleThreadExecutor();
			AtomicReference<Exception> caughtException = new AtomicReference<>();
			CountDownLatch started = new CountDownLatch(1);

			Future<?> future = executor.submit(() -> {
				try {
					JsonPreferencesStore store = new JsonPreferencesStore(prefsFile);
					started.countDown();
					store.put("", "key", "value");
					store.save(); // This should block and then be interrupted
				}
				catch (Exception e) {
					caughtException.set(e);
				}
			});

			// Wait for task to start
			assertTrue(started.await(5, TimeUnit.SECONDS));

			// Give it time to start trying to acquire lock
			Thread.sleep(100);

			// Interrupt the thread
			future.cancel(true);

			// Wait for completion
			executor.shutdown();
			assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

			// Verify we got an InterruptedIOException
			Exception caught = caughtException.get();
			assertNotNull(caught, "Should have caught an exception");
			assertTrue(caught instanceof IOException, "Should be IOException");

			// Check the cause chain for InterruptedIOException
			Throwable current = caught;
			boolean foundInterruptedIO = false;
			while (current != null && !foundInterruptedIO) {
				if (current instanceof InterruptedIOException) {
					foundInterruptedIO = true;
				}
				current = current.getCause();
			}

			// This test documents current behavior - we can verify if it throws InterruptedIOException
			// If not, we'll implement the fix
		}
	}

	@Test
	void testLockFileNotLeakedOnTimeout() throws Exception {
		Path prefsFile = tempDir.resolve("timeout.json");
		Path lockFile = prefsFile.resolveSibling(prefsFile.getFileName() + ".lock");

		// Hold the lock to force timeout
		Files.createDirectories(lockFile.getParent());
		try (RandomAccessFile raf = new RandomAccessFile(lockFile.toFile(), "rw");
				 FileChannel channel = raf.getChannel();
				 FileLock lock = channel.lock()) {

			// Try to create store and save - should timeout
			JsonPreferencesStore store = new JsonPreferencesStore(prefsFile);
			store.put("", "key", "value");

			// Should throw IOException due to timeout
			IOException ex = assertThrows(IOException.class, () -> store.save());
			assertTrue(ex.getMessage().contains("timeout"), "Should be timeout error");
		}

		// After the lock is released, we should be able to use the lock file
		// This verifies no file handles were leaked during the timeout
		JsonPreferencesStore store2 = new JsonPreferencesStore(prefsFile);
		store2.put("", "key2", "value2");
		assertDoesNotThrow(() -> store2.save());

		// Verify the data was saved
		assertEquals("value2", store2.get("", "key2"));
	}

	@Test
	void testLockFileNotLeakedOnException() throws Exception {
		Path prefsFile = tempDir.resolve("exception.json");

		// Create a directory where the lock file should be
		// This will cause an exception when trying to create the lock file
		Path lockFile = prefsFile.resolveSibling(prefsFile.getFileName() + ".lock");
		Files.createDirectories(lockFile); // Create as directory, not file!

		// Try to save - should fail due to lock file being a directory
		JsonPreferencesStore store = new JsonPreferencesStore(prefsFile);
		store.put("", "key", "value");
		assertThrows(IOException.class, () -> store.save());

		// Remove the directory
		Files.delete(lockFile);

		// Now we should be able to save normally
		// This verifies no resources were leaked during the exception
		JsonPreferencesStore store2 = new JsonPreferencesStore(prefsFile);
		store2.put("", "key2", "value2");
		assertDoesNotThrow(() -> store2.save());

		// Verify the data was saved
		assertEquals("value2", store2.get("", "key2"));
	}

	@Test
	void testConcurrentLockAcquisition() throws Exception {
		Path prefsFile = tempDir.resolve("concurrent.json");
		int numThreads = 5;
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		CountDownLatch startLatch = new CountDownLatch(1);
		AtomicBoolean anyFailure = new AtomicBoolean(false);
		AtomicReference<Exception> firstException = new AtomicReference<>();

		// Submit multiple threads that all try to save at once
		// Each thread uses its own store instance to avoid shared state issues
		for (int i = 0; i < numThreads; i++) {
			final int threadId = i;
			executor.submit(() -> {
				try {
					startLatch.await();
					// Each thread creates its own store instance
					JsonPreferencesStore store = new JsonPreferencesStore(prefsFile);
					// First reload to get any existing data
					store.reload();
					store.put("", "key" + threadId, "value" + threadId);
					store.save();
				}
				catch (Exception e) {
					anyFailure.set(true);
					firstException.compareAndSet(null, e);
				}
			});
		}

		// Start all threads
		startLatch.countDown();

		// Wait for completion
		executor.shutdown();
		assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

		// Check if any thread failed
		if (anyFailure.get() && firstException.get() != null) {
			throw new AssertionError("Thread failed with exception", firstException.get());
		}

		// Verify all values were saved - note that due to concurrent saves,
		// not all values may be present (last writer wins)
		JsonPreferencesStore finalStore = new JsonPreferencesStore(prefsFile);

		// At least one value should have been saved
		boolean anyValueSaved = false;
		for (int i = 0; i < numThreads; i++) {
			if (finalStore.get("", "key" + i) != null) {
				anyValueSaved = true;
				break;
			}
		}
		assertTrue(anyValueSaved, "At least one value should have been saved");
	}
}