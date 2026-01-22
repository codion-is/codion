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

import is.codion.common.utilities.property.PropertyValue;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static is.codion.common.model.preferences.PreferencesPath.userPreferencesPath;
import static is.codion.common.utilities.Configuration.stringValue;
import static java.util.Objects.requireNonNull;

/**
 * <p>A file-based preferences implementation without length restrictions.
 * <p>Supports hierarchical preferences through nested JSON structure.
 * <p>Clearing the preferences instance and flushing deletes the underlying file.
 * @see #filePreferences(String)
 * @see #PREFERENCES_LOCATION
 */
public final class FilePreferences extends AbstractPreferences {

	private static final Logger LOG = LoggerFactory.getLogger(FilePreferences.class);

	/**
	 * Provides a way to override the default (OS dependent) directory used to store the user preferences file.
	 * <p>Note that changing this value after a file based preferences instance has been created has no effect on subsequent instances.
	 * <ul>
	 * <li>Value type: String
	 * <li>Default value: null
	 * </ul>
	 * @see #filePreferences(String)
	 */
	public static final PropertyValue<String> PREFERENCES_LOCATION = stringValue("codion.preferences.location");

	private static final Map<Path, FilePreferences> FILE_PREFERENCES = new ConcurrentHashMap<>();

	private static final String[] EMPTY_STRING_ARRAY = new String[0];
	private static final String PATH_SEPARATOR = "/";

	private final JsonPreferencesStore store;
	private final String path;

	private FilePreferences(Path path) {
		this(null, "", new JsonPreferencesStore(path));
		LOG.info("Created file preferences: {}", path);
	}

	FilePreferences(JsonPreferencesStore store) {
		this(null, "", store);
		LOG.debug("Created file preferences with custom store");
	}

	private FilePreferences(@Nullable FilePreferences parent, String name, JsonPreferencesStore store) {
		super(parent, name);
		this.store = store;
		this.path = parent == null ? "" : (parent.path.isEmpty() ? name : parent.path + PATH_SEPARATOR + name);
	}

	@Override
	public void put(String key, String value) {
		requireNonNull(key, "key cannot be null");
		requireNonNull(value, "value cannot be null");
		synchronized (lock) {
			putSpi(key, value);
		}
	}

	/**
	 * @param filename the preferences filename
	 * @return a file based Preferences instance based on the given filename
	 */
	public static Preferences filePreferences(String filename) {
		return FILE_PREFERENCES.computeIfAbsent(userPreferencesPath(filename), FilePreferences::new);
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
}