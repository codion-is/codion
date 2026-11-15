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
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.System.currentTimeMillis;
import static java.util.Objects.requireNonNull;

/**
 * File persistence for a {@link JsonPreferences} in-memory store: thread-safe, multi-JVM safe JSON file storage using
 * file locking for concurrent access and atomic writes. The in-memory storage itself (get/put/child nodes) lives in
 * {@link JsonPreferences}; this class adds loading, saving and reloading of the backing file.
 * <p>
 * If the preferences file becomes corrupted (invalid JSON), this implementation
 * will automatically create a backup of the corrupted file with a ".corrupt.{timestamp}"
 * suffix and start with empty preferences. A file found corrupt when reloaded, having been read
 * before, is backed up likewise and what the store holds is kept, being the last good copy.
 * Either way the corrupt file is replaced at the next save or sync. The corruption is logged.
 * <p>
 * This implementation prevents data loss by throwing an {@link IllegalStateException}
 * when attempting to create a node at a path where a value already exists. For example,
 * if "foo" contains a string value, attempting to create "foo/bar" will fail.
 */
final class JsonPreferencesStore {

	private static final Logger LOG = LoggerFactory.getLogger(JsonPreferencesStore.class);

	private static final long LOCK_TIMEOUT_MS = 5000; // 5 seconds
	private static final long LOCK_RETRY_DELAY_MS = 50; // 50ms between retries
	private static final String TEMP_FILE_SUFFIX = ".tmp";

	private final Path filePath;
	private final Path lockFilePath;
	private final boolean prettyPrint;
	private final JsonPreferences preferences = new JsonPreferences(true);

	/**
	 * Serializes this JVM's own access to the file, so that the {@link FileLock} below only ever arbitrates
	 * between JVMs. A {@link FileLock} is held on behalf of the whole JVM and is explicitly documented as
	 * unsuitable for controlling access between threads within one: two threads locking the same region
	 * through different channels get an {@link OverlappingFileLockException}, whichever of them asked for a
	 * shared lock. Every node of a file shares one store, and {@code flushSpi} runs under the per-node lock,
	 * so concurrent saves are ordinary rather than exotic.
	 * <p>
	 * Mutual exclusion rather than a read/write lock: concurrent readers would gain nothing, since they would
	 * still each need their own {@link FileLock} and collide on it exactly as writers do.
	 */
	private final Lock lock = new ReentrantLock();

	private int saves = 0;

	private volatile long lastModified;
	//each save creates a new file, so the file key, the inode where available, changes with every save,
	//where the modification time, coarse grained, might not
	private volatile @Nullable Object fileKey;
	//the file has been found corrupt and backed up, to be replaced by what the store holds at the next save or sync
	private boolean corrupt = false;

	JsonPreferencesStore(Path filePath) {
		this(filePath, false);
	}

	JsonPreferencesStore(Path filePath, boolean prettyPrint) {
		this.filePath = requireNonNull(filePath);
		this.lockFilePath = filePath.resolveSibling(filePath.getFileName() + ".lock");
		this.prettyPrint = prettyPrint;
		LOG.debug("Initializing preferences store at {}", filePath);
		try {
			loadData(false, false);
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

	/**
	 * Saves the preferences to disk using atomic write with file locking.
	 * If the preferences are empty, the file is deleted instead of written.
	 * @throws IOException if an I/O error occurs
	 */
	void save() throws IOException {
		lock.lock();
		try (FileLock fileLock = acquireExclusiveLock()) {
			write();
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * Writes the preferences, or deletes the file if they are empty, the caller holding the file lock.
	 * The lock file is deliberately left in place when the file is deleted: deleting it while another
	 * thread or process holds a {@link FileLock} on it unlinks the inode they locked, so the next
	 * {@code acquireLock()} creates and locks a different file and mutual exclusion silently fails.
	 */
	private void write() throws IOException {
		saves++;
		// taken under the file lock, after the reload, so that it holds what any other JVM has saved
		JsonPreferences.Snapshot snapshot = preferences.snapshot(prettyPrint);
		String content = snapshot.content;
		if (content == null) {
			LOG.debug("Preferences empty, deleting file if exists: {}", filePath);
			Files.deleteIfExists(filePath);
			fileGone();
			preferences.saved(snapshot.changes);
			return;
		}
		LOG.debug("Saving preferences to {}", filePath);
		long startTime = currentTimeMillis();
		Files.createDirectories(filePath.getParent());
		// Write to temp file first, named after the file, so that one left behind can be told from those of other files
		String tempFilePrefix = filePath.getFileName() + ".";
		removeTempFiles(tempFilePrefix);
		Path tempFile = Files.createTempFile(filePath.getParent(), tempFilePrefix, TEMP_FILE_SUFFIX);
		try {
			Files.write(tempFile.toAbsolutePath(), content.getBytes(StandardCharsets.UTF_8));
			// Force sync to disk before atomic move (helps on Windows/VirtualBox)
			try (FileChannel channel = FileChannel.open(tempFile, StandardOpenOption.WRITE)) {
				channel.force(true);
			}
			atomicMove(tempFile, filePath);
			stamp();
			corrupt = false;
			preferences.saved(snapshot.changes);
			LOG.trace("Preferences saved successfully in {} ms", currentTimeMillis() - startTime);
		}
		finally {
			Files.deleteIfExists(tempFile);
		}
	}

	/**
	 * Removes the temporary files of this file left behind by a JVM that ended between creating one and moving it.
	 * The caller holds the file lock, so none of them belongs to a save in progress. Best effort.
	 */
	private void removeTempFiles(String prefix) {
		try (DirectoryStream<Path> tempFiles = Files.newDirectoryStream(filePath.getParent(), file -> tempFile(file, prefix))) {
			for (Path tempFile : tempFiles) {
				Files.deleteIfExists(tempFile);
				LOG.debug("Removed temporary file left behind: {}", tempFile);
			}
		}
		catch (IOException | DirectoryIteratorException e) {
			LOG.debug("Unable to remove temporary files left behind for {}", filePath, e);
		}
	}

	/**
	 * @return true if the file is named as a temporary file of this file is: the prefix, digits and the suffix
	 */
	private static boolean tempFile(Path file, String prefix) {
		String name = file.getFileName().toString();
		if (name.length() <= prefix.length() + TEMP_FILE_SUFFIX.length() || !name.startsWith(prefix) || !name.endsWith(TEMP_FILE_SUFFIX)) {
			return false;
		}
		for (int i = prefix.length(); i < name.length() - TEMP_FILE_SUFFIX.length(); i++) {
			if (!Character.isDigit(name.charAt(i))) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Reloads the file if it has been modified since it was last read or written, re-applying the changes made
	 * since on top of it, and saves those changes, the file is left untouched when there are none, unless it
	 * has been found corrupt, what the store holds then replacing it. This is what
	 * the JDK's own file backed implementation does for both {@code flush()} and {@code sync()}.
	 * <p>The file lock is held from the reload to the write, so that another JVM can not save in between,
	 * its changes then replaced by a file that never contained them.
	 * @throws IOException in case of an I/O error
	 */
	void sync() throws IOException {
		lock.lock();
		try {
			if (!preferences.modified()) {
				reload(false);
			}
			// a file found corrupt is replaced by what the store holds, changes or none
			if (preferences.modified() || corrupt) {
				try (FileLock fileLock = acquireExclusiveLock()) {
					reload(true);
					// unless another JVM has replaced the corrupt file in the meantime, with nothing to add to it
					if (preferences.modified() || corrupt) {
						write();
					}
				}
			}
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * @return the number of times {@link #save()} has been called, for tests
	 */
	int saves() {
		lock.lock();
		try {
			return saves;
		}
		finally {
			lock.unlock();
		}
	}

	void reload() throws IOException {
		reload(false);
	}

	/**
	 * @param fileLocked true if the caller holds the file lock
	 */
	private void reload(boolean fileLocked) throws IOException {
		// held across the check and the load, so two threads seeing the same modification time do not both reload
		lock.lock();
		try {
			// no exists() check first: the file can be deleted between the check and the read - by save()
			// when the preferences are emptied, or by another process - so ask for the timestamp and treat
			// its absence as the answer, rather than racing between the two calls
			try {
				if (modifiedExternally()) {
					LOG.debug("File has been modified externally, reloading from {}", filePath);
					loadData(true, fileLocked);
				}
				else {
					LOG.trace("File has not been modified, skipping reload");
				}
			}
			catch (NoSuchFileException e) {
				if (lastModified == 0) {
					LOG.trace("File does not exist, nothing to reload");
				}
				else {
					// deleted since it was last read or written, the changes made since apply to an empty file
					LOG.debug("File has been deleted externally, {}", filePath);
					missing(true);
				}
			}
		}
		finally {
			lock.unlock();
		}
	}

	private void loadData(boolean merge, boolean fileLocked) throws IOException {
		lock.lock();
		try {
			if (!Files.exists(filePath)) {
				// fast path only, so the common "no preferences yet" case costs no lock; this is not the
				// authoritative check, the file may be deleted at any point after it returns true, which is
				// what the NoSuchFileException in read() is for
				LOG.trace("Preferences file does not exist, starting with empty preferences");
				missing(merge);
			}
			else if (fileLocked) {
				read(merge);
			}
			else {
				try (FileLock fileLock = acquireSharedLock()) {
					read(merge);
				}
			}
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * Reads the file, the caller holding the file lock.
	 */
	private void read(boolean merge) throws IOException {
		LOG.trace("Loading preferences from {}", filePath);
		String content;
		try {
			content = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
		}
		catch (NoSuchFileException e) {
			// as in reload(): no exists() check, the file may be deleted concurrently
			LOG.trace("Preferences file does not exist, starting with empty preferences");
			missing(merge);
			return;
		}

		try {
			if (merge) {
				preferences.merge(content);
			}
			else {
				preferences.load(content);
			}
			stamp();
			corrupt = false;
			LOG.trace("Loaded preferences from file");
		}
		catch (NoSuchFileException e) {
			// deleted between the read and the timestamp; the content read above is still valid
			fileGone();
		}
		catch (JSONException e) {
			// Invalid JSON format
			LOG.error("Preferences file contains invalid JSON: {}", filePath, e);
			handleCorruptedFile("Invalid JSON format", e, merge);
		}
	}

	/**
	 * The file is missing or unreadable, when merging the changes made since it was last read or written apply to an empty file.
	 */
	private boolean modifiedExternally() throws IOException {
		BasicFileAttributes attributes = Files.readAttributes(filePath, BasicFileAttributes.class);

		return attributes.lastModifiedTime().toMillis() != lastModified || !Objects.equals(attributes.fileKey(), fileKey);
	}

	private void stamp() throws IOException {
		BasicFileAttributes attributes = Files.readAttributes(filePath, BasicFileAttributes.class);
		lastModified = attributes.lastModifiedTime().toMillis();
		fileKey = attributes.fileKey();
	}

	private void fileGone() {
		lastModified = 0;
		fileKey = null;
		corrupt = false;
	}

	private void missing(boolean merge) {
		fileGone();
		if (merge) {
			preferences.merge("{}");
		}
		else {
			preferences.clear();
		}
	}

	private void handleCorruptedFile(String reason, Exception cause, boolean merge) throws IOException {
		// Backup corrupted file
		Path backupPath = filePath.resolveSibling(filePath.getFileName() + ".corrupt." + currentTimeMillis());
		Files.copy(filePath, backupPath, StandardCopyOption.REPLACE_EXISTING);

		if (!merge) {
			// nothing has been read, starting with empty data
			preferences.clear();
		}
		// when merging what the store holds is kept, the last good copy of the file along with the changes made since
		try {
			// stamped, so that the same file is not taken for modified and backed up again
			stamp();
			corrupt = true;
		}
		catch (NoSuchFileException e) {
			fileGone();
		}

		LOG.warn("Corrupted preferences file detected at {}, reason: {}, backup saved to: {}, {}",
						filePath, reason, backupPath, merge ? "keeping the preferences held" : "starting with empty preferences", cause);
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
					FileLock fileLock = shared ? channel.tryLock(0, Long.MAX_VALUE, true) : channel.tryLock();
					if (fileLock != null) {
						// Success - resources will be closed by FileLockWrapper
						RandomAccessFile successFile = lockFile;
						lockFile = null; // Prevent cleanup in finally block
						// and the channel likewise: closing a FileChannel releases every lock held on it,
						// so closing it here would hand back a lock that was already released
						channel = null;
						LOG.trace("Lock acquired successfully after {} retries", retryCount);
						return new FileLockWrapper(fileLock, successFile);
					}
				}
				catch (OverlappingFileLockException e) {
					// This store's own callers are serialized by the in-JVM lock, so reaching here means a second
					// store instance on the same file - two paths that differ textually but resolve to one file,
					// which FilePreferences caches separately. Rare, and the retry below still resolves it.
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