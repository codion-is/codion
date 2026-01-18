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

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;

import static java.util.Objects.requireNonNull;

/**
 * A file-based preferences implementation without length restrictions.
 * Supports hierarchical preferences through nested JSON structure.
 */
final class FilePreferences extends AbstractPreferences {

	private static final Logger LOG = LoggerFactory.getLogger(FilePreferences.class);

	private static final String[] EMPTY_STRING_ARRAY = new String[0];
	private static final String PATH_SEPARATOR = "/";

	private final JsonPreferencesStore store;
	private final String path;

	FilePreferences(String filename) throws IOException {
		this(null, "", createDefaultStore(requireNonNull(filename)));
		LOG.info("Created file preferences: {}", filename);
	}

	// Package-private constructor for testing
	FilePreferences(JsonPreferencesStore store) {
		this(null, "", store);
		LOG.debug("Created file preferences with custom store");
	}

	private FilePreferences(@Nullable FilePreferences parent, String name, JsonPreferencesStore store) {
		super(parent, name);
		this.store = store;
		this.path = parent == null ? "" : (parent.path.isEmpty() ? name : parent.path + PATH_SEPARATOR + name);
	}

	private static JsonPreferencesStore createDefaultStore(String filename) throws IOException {
		return new JsonPreferencesStore(PreferencesPath.userPreferencesPath(filename));
	}

	@Override
	public void put(String key, String value) {
		// Override to bypass AbstractPreferences validation
		synchronized (lock) {
			if (key == null) {
				throw new NullPointerException("key cannot be null");
			}
			if (value == null) {
				throw new NullPointerException("value cannot be null");
			}
			putSpi(key, value);
		}
	}

	@Override
	protected void putSpi(String key, String value) {
		store.put(path, key, value);
	}

	@Override
	protected @Nullable String getSpi(String key) {
		return store.get(path, key);
	}

	@Override
	protected void removeSpi(String key) {
		LOG.trace("Removing preference key '{}' at path '{}'", key, path);
		store.remove(path, key);
	}

	@Override
	protected String[] keysSpi() throws BackingStoreException {
		return store.keys(path).toArray(EMPTY_STRING_ARRAY);
	}

	@Override
	protected void flushSpi() throws BackingStoreException {
		LOG.debug("Flushing preferences to disk");
		try {
			store.save();
		}
		catch (IOException e) {
			LOG.error("Failed to flush preferences", e);
			throw new BackingStoreException(e);
		}
	}

	@Override
	protected void syncSpi() throws BackingStoreException {
		LOG.debug("Syncing preferences from disk");
		try {
			store.reload();
		}
		catch (IOException e) {
			LOG.error("Failed to sync preferences", e);
			throw new BackingStoreException(e);
		}
	}

	@Override
	protected String[] childrenNamesSpi() {
		return store.childrenNames(path).toArray(EMPTY_STRING_ARRAY);
	}

	@Override
	protected AbstractPreferences childSpi(String name) {
		LOG.trace("Creating child preference node '{}' under path '{}'", name, path);
		return new FilePreferences(this, name, store);
	}

	@Override
	protected void removeNodeSpi() throws BackingStoreException {
		LOG.debug("Removing preference node at path '{}'", path);
		store.removeNode(path);
		try {
			store.save();
		}
		catch (IOException e) {
			LOG.error("Failed to remove preference node", e);
			throw new BackingStoreException(e);
		}
	}

	void delete() throws IOException {
		store.delete();
	}
}