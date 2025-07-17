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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

/**
 * A thread-safe, multi-JVM safe JSON-based storage for preferences.
 * Uses file locking for concurrent access and atomic writes.
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

	private static final String PATH_SEPARATOR = "/";
	private static final long LOCK_TIMEOUT_MS = 5000; // 5 seconds
	private static final long LOCK_RETRY_DELAY_MS = 50; // 50ms between retries
	private static final String NODE = ".node";

	private final Path filePath;
	private final Path lockFilePath;
	private final Lock inMemoryLock = new Lock() {};

	private final JSONObject data = new JSONObject();

	private long lastModified;

	JsonPreferencesStore(Path filePath) throws IOException {
		this.filePath = requireNonNull(filePath);
		this.lockFilePath = filePath.resolveSibling(filePath.getFileName() + ".lock");
		LOG.debug("Initializing preferences store at {}", filePath);
		loadData();
	}

	void put(String path, String key, String value) {
		requireNonNull(path);
		requireNonNull(key);
		requireNonNull(value);

		synchronized (inMemoryLock) {
			LOG.trace("Putting value at path '{}', key '{}'", path, key);
			JSONObject node = getOrCreateNode(path);

			// Try to parse the value as JSON
			Object valueToStore = tryParseJson(value);
			if (valueToStore != null) {
				// Valid JSON - store as nested object/array
				node.put(key, valueToStore);
				LOG.trace("Stored value as JSON object/array for key '{}'", key);
			}
			else {
				// Not valid JSON - store as string
				node.put(key, value);
			}
		}
	}

	@Nullable String get(String path, String key) {
		requireNonNull(path);
		requireNonNull(key);

		synchronized (inMemoryLock) {
			JSONObject node = getNode(path);
			if (node == null || !node.has(key)) {
				return null;
			}

			Object value = node.get(key);
			if (value instanceof String) {
				return (String) value;
			}
			else if (value instanceof JSONObject || value instanceof JSONArray) {
				// Convert JSON object/array back to string
				return value.toString();
			}
			else {
				// Handle other types (numbers, booleans)
				return String.valueOf(value);
			}
		}
	}

	void remove(String path, String key) {
		requireNonNull(path);
		requireNonNull(key);

		synchronized (inMemoryLock) {
			LOG.trace("Removing key '{}' at path '{}'", key, path);
			JSONObject node = getNode(path);
			if (node != null) {
				node.remove(key);
			}
		}
	}

	Set<String> keys(String path) {
		requireNonNull(path);

		synchronized (inMemoryLock) {
			JSONObject node = getNode(path);
			if (node == null) {
				return emptySet();
			}

			// Return all keys except those that are child nodes (JSONObjects used for hierarchy)
			// Note: JSONObjects/JSONArrays that represent stored JSON values should be included
			return node.keySet().stream()
							.filter(k -> !isChildNode(node, k) && !NODE.equals(k))
							.collect(toSet());
		}
	}

	Set<String> childrenNames(String path) {
		requireNonNull(path);

		synchronized (inMemoryLock) {
			JSONObject node = getNode(path);
			if (node == null) {
				return emptySet();
			}

			// Return only child nodes (JSONObjects used for hierarchy)
			return node.keySet().stream()
							.filter(k -> isChildNode(node, k))
							.collect(toSet());
		}
	}

	void removeNode(String path) {
		requireNonNull(path);

		synchronized (inMemoryLock) {
			LOG.debug("Removing node at path '{}'", path);
			if (path.isEmpty()) {
				// Clear root node
				data.keySet().clear();
			}
			else {
				int lastSeparator = path.lastIndexOf(PATH_SEPARATOR);
				if (lastSeparator >= 0) {
					String parentPath = path.substring(0, lastSeparator);
					String nodeName = path.substring(lastSeparator + 1);
					JSONObject parent = getNode(parentPath);
					if (parent != null) {
						parent.remove(nodeName);
					}
				}
				else {
					// Direct child of root
					data.remove(path);
				}
			}
		}
	}

	/**
	 * Saves the preferences to disk using atomic write with file locking.
	 * @throws IOException if an I/O error occurs
	 */
	void save() throws IOException {
		synchronized (inMemoryLock) {
			LOG.debug("Saving preferences to {}", filePath);
			long startTime = currentTimeMillis();
			Files.createDirectories(filePath.getParent());
			// Write to temp file first
			Path tempFile = Files.createTempFile(filePath.getParent(), "prefs", ".tmp");
			try {
				Files.write(tempFile.toAbsolutePath(), data.toString(2).getBytes()); // Pretty print
				// Atomic move with lock
				try (FileLock lock = acquireExclusiveLock()) {
					Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
					lastModified = Files.getLastModifiedTime(filePath).toMillis();
					LOG.trace("Preferences saved successfully in {} ms", currentTimeMillis() - startTime);
				}
			}
			finally {
				Files.deleteIfExists(tempFile);
			}
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
		synchronized (inMemoryLock) {
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
					// Clear existing data and reload
					data.keySet().clear();
					JSONObject reloaded = new JSONObject(content);
					for (String key : reloaded.keySet()) {
						data.put(key, reloaded.get(key));
					}
					lastModified = Files.getLastModifiedTime(filePath).toMillis();
					LOG.trace("Loaded {} keys from preferences file", this.data.length());
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
		data.keySet().clear();
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

	private @Nullable JSONObject getNode(String path) {
		if (path.isEmpty()) {
			return data;
		}
		String[] parts = path.split(PATH_SEPARATOR);
		JSONObject current = data;
		for (String part : parts) {
			if (!current.has(part) || !(current.get(part) instanceof JSONObject)) {
				return null;
			}
			current = current.getJSONObject(part);
		}

		return current;
	}

	private JSONObject getOrCreateNode(String path) {
		if (path.isEmpty()) {
			return data;
		}
		String[] parts = path.split(PATH_SEPARATOR);
		JSONObject current = data;
		for (String part : parts) {
			if (!current.has(part)) {
				JSONObject newNode = new JSONObject();
				// Mark this as a preferences node, not a stored JSON value
				newNode.put(NODE, true);
				current.put(part, newNode);
			}
			else if (!(current.get(part) instanceof JSONObject)) {
				// A value exists at this path, cannot create a node
				LOG.error("Cannot create node '{}' at path '{}' because a value already exists", part, path);
				throw new IllegalStateException("Cannot create node '" + part +
								"' because a value already exists at this path");
			}
			current = current.getJSONObject(part);
		}

		return current;
	}

	/**
	 * Attempts to parse a string value as JSON.
	 * @param value the string to parse
	 * @return a JSONObject or JSONArray if the value is valid JSON, null otherwise
	 */
	private static @Nullable Object tryParseJson(String value) {
		// Quick check to avoid parsing non-JSON strings
		if (value == null || value.isEmpty()) {
			return null;
		}

		String trimmed = value.trim();
		if (trimmed.isEmpty()) {
			return null;
		}

		// Only try to parse if it looks like JSON (starts with { or [)
		char firstChar = trimmed.charAt(0);
		if (firstChar != '{' && firstChar != '[') {
			return null;
		}

		try {
			if (firstChar == '{') {
				return new JSONObject(value);
			}
			else {
				return new JSONArray(value);
			}
		}
		catch (JSONException e) {
			// Not valid JSON, return null to store as string
			return null;
		}
	}

	/**
	 * Checks if a key represents a child node (used for preferences hierarchy)
	 * rather than a stored value.
	 */
	private static boolean isChildNode(JSONObject parent, String key) {
		Object value = parent.get(key);
		if (!(value instanceof JSONObject)) {
			return false;
		}

		JSONObject obj = (JSONObject) value;

		// A node is marked with the special ".node" property
		return obj.optBoolean(NODE, false);
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

	private interface Lock {}
}