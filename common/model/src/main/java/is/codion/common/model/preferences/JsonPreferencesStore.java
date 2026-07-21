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

import org.json.JSONException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.lang.System.currentTimeMillis;
import static java.util.Objects.requireNonNull;

/**
 * File persistence for a {@link JsonPreferences} in-memory store: thread-safe, multi-JVM safe JSON file storage using
 * file locking for concurrent access and atomic writes. The in-memory storage itself (get/put/child nodes) lives in
 * {@link JsonPreferences}; this class adds loading, saving and reloading of the backing file.
 * <p>
 * If the preferences file becomes corrupted (invalid JSON), this implementation
 * will automatically create a backup of the corrupted file with a ".corrupt.{timestamp}"
 * suffix and start with empty preferences. The corruption is logged to System.err.
 * <p>
 * This implementation prevents data loss by throwing an {@link IllegalStateException}
 * when attempting to create a node at a path where a value already exists. For example,
 * if "foo" contains a string value, attempting to create "foo/bar" will fail.
 */
final class JsonPreferencesStore {

	private static final Logger LOG = LoggerFactory.getLogger(JsonPreferencesStore.class);

	private static final long LOCK_TIMEOUT_MS = 5000; // 5 seconds
	private static final long LOCK_RETRY_DELAY_MS = 50; // 50ms between retries

	private final Path filePath;
	private final Path lockFilePath;
	private final boolean prettyPrint;
	private final JsonPreferences preferences = new JsonPreferences();

	private volatile long lastModified;

	JsonPreferencesStore(Path filePath) {
		this(filePath, false);
	}

	JsonPreferencesStore(Path filePath, boolean prettyPrint) {
		this.filePath = requireNonNull(filePath);
		this.lockFilePath = filePath.resolveSibling(filePath.getFileName() + ".lock");
		this.prettyPrint = prettyPrint;
		LOG.debug("Initializing preferences store at {}", filePath);
		try {
			loadData();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	JsonPreferences preferences() {
		return preferences;
	}

	void put(String path, String key, String value) {
		preferences.put(path, key, value);
	}

	@Nullable String get(String path, String key) {
		return preferences.get(path, key);
	}

	void remove(String path, String key) {
		preferences.remove(path, key);
	}

	Set<String> keys(String path) {
		return preferences.keys(path);
	}

	Set<String> childrenNames(String path) {
		return preferences.childrenNames(path);
	}

	void removeNode(String path) {
		preferences.removeNode(path);
	}

	/**
	 * Saves the preferences to disk using atomic write with file locking.
	 * If the preferences are empty, the file is deleted instead of written.
	 * @throws IOException if an I/O error occurs
	 */
	void save() throws IOException {
		// A point-in-time snapshot of the store, taken atomically; the file write below need not hold the in-memory lock.
		String content = preferences.snapshot(prettyPrint);
		if (content == null) {
			LOG.debug("Preferences empty, deleting file if exists: {}", filePath);
			delete();
			return;
		}
		LOG.debug("Saving preferences to {}", filePath);
		long startTime = currentTimeMillis();
		Files.createDirectories(filePath.getParent());
		// Write to temp file first
		Path tempFile = Files.createTempFile(filePath.getParent(), "prefs", ".tmp");
		try {
			Files.write(tempFile.toAbsolutePath(), content.getBytes());
			// Force sync to disk before atomic move (helps on Windows/VirtualBox)
			try (FileChannel channel = FileChannel.open(tempFile, StandardOpenOption.WRITE)) {
				channel.force(true);
			}
			// Atomic move with lock
			try (FileLock lock = acquireExclusiveLock()) {
				atomicMove(tempFile, filePath);
				lastModified = Files.getLastModifiedTime(filePath).toMillis();
				LOG.trace("Preferences saved successfully in {} ms", currentTimeMillis() - startTime);
			}
		}
		finally {
			Files.deleteIfExists(tempFile);
		}
	}

	/**
	 * Reloads the preferences from disk if the file has been modified externally.
	 * If the preferences file becomes corrupted (invalid JSON), this method will
	 * automatically create a backup of the corrupted file with a ".corrupt.{timestamp}"
	 * suffix and continue with empty preferences.
	 * @throws IOException if an I/O error occurs
	 */
	void reload() throws IOException {
		if (Files.exists(filePath)) {
			long currentModified = Files.getLastModifiedTime(filePath).toMillis();
			if (currentModified != lastModified) {
				LOG.debug("File has been modified externally, reloading from {}", filePath);
				loadData();
			}
			else {
				LOG.trace("File has not been modified, skipping reload");
			}
		}
		else {
			LOG.trace("File does not exist, nothing to reload");
		}
	}

	/**
	 * Deletes the prferences file along with the lock file
	 * @throws IOException in case of an exception
	 */
	void delete() throws IOException {
		Files.deleteIfExists(filePath);
		Files.deleteIfExists(lockFilePath);
	}

	private void loadData() throws IOException {
		if (Files.exists(filePath)) {
			LOG.trace("Loading preferences from {}", filePath);
			try (FileLock lock = acquireSharedLock()) {
				String content;
				try {
					content = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
				}
				catch (MalformedInputException e) {
					// File contains binary data or invalid encoding
					LOG.error("Preferences file contains invalid character encoding: {}", filePath, e);
					handleCorruptedFile("File contains invalid character encoding", e);
					return;
				}

				try {
					preferences.load(content);
					lastModified = Files.getLastModifiedTime(filePath).toMillis();
					LOG.trace("Loaded preferences from file");
				}
				catch (JSONException e) {
					// Invalid JSON format
					LOG.error("Preferences file contains invalid JSON: {}", filePath, e);
					handleCorruptedFile("Invalid JSON format", e);
				}
			}
		}
		else {
			LOG.trace("Preferences file does not exist, starting with empty preferences");
			lastModified = 0;
		}
	}

	private void handleCorruptedFile(String reason, Exception cause) throws IOException {
		// Backup corrupted file
		Path backupPath = filePath.resolveSibling(filePath.getFileName() + ".corrupt." + currentTimeMillis());
		Files.copy(filePath, backupPath, StandardCopyOption.REPLACE_EXISTING);

		// Initialize with empty data
		preferences.clear();
		lastModified = 0;

		LOG.warn("Corrupted preferences file detected at {}, reason: {}, backup saved to: {}, starting with empty preferences",
						filePath, reason, backupPath, cause);
	}

	private FileLock acquireSharedLock() throws IOException {
		return acquireLock(true);
	}

	private FileLock acquireExclusiveLock() throws IOException {
		return acquireLock(false);
	}

	private FileLock acquireLock(boolean shared) throws IOException {
		Files.createDirectories(lockFilePath.getParent());
		LOG.trace("Acquiring {} lock on {}", shared ? "shared" : "exclusive", lockFilePath);

		RandomAccessFile lockFile = null;
		FileChannel channel = null;
		try {
			lockFile = new RandomAccessFile(lockFilePath.toFile(), "rw");
			channel = lockFile.getChannel();

			long startTime = currentTimeMillis();
			int retryCount = 0;
			while (currentTimeMillis() - startTime < LOCK_TIMEOUT_MS) {
				try {
					FileLock lock = shared ? channel.tryLock(0, Long.MAX_VALUE, true) : channel.tryLock();
					if (lock != null) {
						// Success - resources will be closed by FileLockWrapper
						RandomAccessFile successFile = lockFile;
						lockFile = null; // Prevent cleanup in finally block
						LOG.trace("Lock acquired successfully after {} retries", retryCount);
						return new FileLockWrapper(lock, successFile);
					}
				}
				catch (OverlappingFileLockException e) {
					// Lock held by another thread in this JVM
					LOG.trace("Lock held by another thread in this JVM, retry {}", ++retryCount);
				}

				try {
					TimeUnit.MILLISECONDS.sleep(LOCK_RETRY_DELAY_MS);
					retryCount++;
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					LOG.debug("Interrupted while waiting for lock on {}", lockFilePath);
					InterruptedIOException ioe = new InterruptedIOException("Interrupted while waiting for lock");
					ioe.initCause(e);
					throw ioe;
				}
			}

			LOG.warn("Failed to acquire file lock on {} within {} ms timeout after {} retries",
							lockFilePath, LOCK_TIMEOUT_MS, retryCount);
			throw new IOException("Failed to acquire file lock within timeout");
		}
		finally {
			// Clean up resources if we're not returning a successful lock
			if (channel != null) {
				try {
					channel.close();
				}
				catch (IOException ignored) {/**/}
			}
			if (lockFile != null) {
				try {
					lockFile.close();
				}
				catch (IOException ignored) {/**/}
			}
		}
	}

	/**
	 * Performs an atomic move from source to target with retry logic for Windows compatibility.
	 * On Windows, AccessDeniedException can occur if the target file has lingering handles
	 * from recent read operations. This method retries with exponential backoff to handle
	 * this platform-specific behavior.
	 * @param source the source file
	 * @param target the target file
	 * @throws IOException if the move fails after all retries
	 */
	private static void atomicMove(Path source, Path target) throws IOException {
		int maxRetries = 10;
		long initialDelayMs = 10;
		for (int attempt = 0; attempt <= maxRetries; attempt++) {
			try {
				Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
				return;
			}
			catch (AccessDeniedException e) {
				if (attempt == maxRetries) {
					LOG.warn("Failed to move {} to {} after {} attempts", source, target, maxRetries + 1);
					throw e;
				}

				// Windows-specific issue: file may still be in use by lingering handles
				// Retry with exponential backoff: 10ms, 20ms, 40ms, 80ms, 160ms, 320ms...
				long delayMs = initialDelayMs * (1L << attempt);
				LOG.trace("Access denied when moving {} to {}, retrying in {}ms (attempt {}/{})",
								source, target, delayMs, attempt + 1, maxRetries);
				try {
					TimeUnit.MILLISECONDS.sleep(delayMs);
				}
				catch (InterruptedException exception) {
					Thread.currentThread().interrupt();
					InterruptedIOException ioException = new InterruptedIOException("Interrupted while retrying file move");
					ioException.initCause(exception);

					throw ioException;
				}
			}
		}
	}

	/**
	 * Wrapper to ensure lock file is closed when lock is released.
	 */
	private static class FileLockWrapper extends FileLock {

		private final FileLock delegate;
		private final RandomAccessFile file;

		FileLockWrapper(FileLock delegate, RandomAccessFile file) {
			super(delegate.channel(), delegate.position(), delegate.size(), delegate.isShared());
			this.delegate = delegate;
			this.file = file;
		}

		@Override
		public boolean isValid() {
			return delegate.isValid();
		}

		@Override
		public void release() throws IOException {
			try {
				if (delegate.isValid()) {
					delegate.release();
				}
			}
			finally {
				file.close();
			}
		}
	}
}